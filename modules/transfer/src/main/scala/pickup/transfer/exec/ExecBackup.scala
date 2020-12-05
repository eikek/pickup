package pickup.transfer.exec

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import java.nio.file.Path
import java.nio.file.attribute.{PosixFilePermission => P}
import org.log4s._
import scala.concurrent.ExecutionContext

import pickup.transfer.data.Files._
import pickup.transfer.Config
import pickup.transfer.data._
import pickup.transfer.store._

final class ExecBackup[F[_]: Sync: ContextShift](cfg: Config
  , xa: Transactor[F]
  , blockingEC: ExecutionContext) extends DoobieMeta {

  private[this] val logger = getLogger

  def exec(peer: OutgoingPeer): F[Option[Output]] =
    for {
      dev <- Device.loadEnabled.transact(xa).map(_.filter(_.enabled))
      out <- dev.map(d => exec(peer.id)(d, peer.targetUri(d)).map(Some(_))).getOrElse(None.pure[F])
    } yield out


  def exec(id: String)(dev: Device, targetUri: Uri): F[Output] = {
    exec0(id)(dev, targetUri).flatMap { out =>
      if (out.success) cleanup(id)(dev, targetUri).map(_ => out)
      else out.pure[F]
    }
  }

  def restore(peer: OutgoingPeer, daysBack: Option[Int]): F[Option[Output]] =
    for {
      dev <- Device.loadEnabled.transact(xa).map(_.filter(_.enabled))
      out <- dev.map(d => restore(peer.id)(d, peer.targetUri(d), daysBack).map(Some(_))).getOrElse(None.pure[F])
    } yield out

  def restore(id: String)(dev: Device, targetUri: Uri, daysBack: Option[Int]): F[Output] = {
    val args = basicOptions ++
      cfg.restoreArgs ++
      daysBack.map(d => Seq("-t", d+"D")).getOrElse(Seq.empty) ++ Seq(
        targetUri.asString,
        (cfg.personal.root / "_restore_" / id).toString
      )
    runDuplicity("restore-"+ id, args, dev, targetUri, false)
  }

  private def exec0(id: String)(dev: Device, targetUri: Uri): F[Output] = {
    val args = basicOptions ++ cfg.backupArgs ++ Seq(
      "--exclude", (cfg.personal.root / "_restore_").toString,
      cfg.personal.root.toString,
      targetUri.asString
    )
    runDuplicity(id, args, dev, targetUri, false)
  }

  private def cleanup(id: String)(dev: Device, targetUri: Uri): F[Unit] = {
    val args = basicOptions ++ cfg.cleanupArgs ++ Seq(
      "--force",
      targetUri.asString
    )
    runDuplicity(id, args, dev, targetUri, true).map(_ => ())
  }

  private def basicOptions: Seq[String] = {
    val temp = cfg.workingDir/"temp"
    val sshId = cfg.workingDir/".ssh"/"id_rsa"
    Seq(
      "--gpg-binary", cfg.gpgCmd,
      "--gpg-options", "--trust-model=always",
      "--tempdir", temp.toString,
      "--name", "pickup-backup",
      "--ssh-options", s"-oIdentityFile=${sshId} -oStrictHostKeyChecking=no"
    )
  }

  private def runDuplicity(id: String, args: Seq[String], dev: Device, targetUri: Uri, attach: Boolean): F[Output] = {
    val temp = cfg.workingDir/"temp"
    val process: F[Output] =
      ContextShift[F].evalOn(blockingEC)(Sync[F].delay(
        Exec.execute(id)(cfg.duplicityCmd, args, cfg.workingDir, Map("PASSPHRASE" -> dev.password.value), attach))
      )
    for {
      _   <- temp.mkdirs[F]
      _   <- Sync[F].delay(logger.info(s"Executing (cleanup) '${cfg.duplicityCmd} ${args.mkString(" ")}'"))
      _   <- prepareHome(dev)
      _   <- knownHostsFile(targetUri)
      out <- process
    } yield out
  }


  def findOutput(peer: OutgoingPeer): Option[Output] =
    Exec.findOutput(cfg.workingDir, peer.id)

  def findRestoreOutput(peer: OutgoingPeer): Option[Output] =
    Exec.findOutput(cfg.workingDir, "restore-" + peer.id)

  private def knownHostsFile(target: Uri): F[Unit] = {
    val knownHosts = cfg.workingDir/".ssh"/"known_hosts"
    val tries = List(target.hostAndPort, s"[${target.host}]" + Uri.ToString(target).port)
    val exists = knownHosts.lines[F](blockingEC).
      filter(s => !s.trim.isEmpty && !s.startsWith("#")).
      exists(s => tries.exists(s.startsWith)).
      compile.last.map(_.getOrElse(false))
    val add = Sync[F].delay(Exec.runAppend(
      cfg.keyscanCmd,
      target.port.map(p => Seq("-p", p.toString)).getOrElse(Seq.empty) ++ Seq(target.host),
      cfg.workingDir)(knownHosts))

    exists.flatMap {
      case true =>
        ().pure[F]
      case false =>
        Sync[F].delay(logger.info(s"Adding host key of ${target.hostAndPort} to known_hosts file")) >> add.map(_ => ())
    }
  }

  private def prepareHome(dev: Device): F[Unit] = {
    val home = cfg.workingDir
    val dotSsh = home/".ssh"

    if (dotSsh.notExists) makeSshConfig(dev, dotSsh)
    else ().pure[F]
  }

  private def makeSshConfig(dev: Device, dotSsh: Path): F[Unit] = {
    val idrsa = dotSsh/"id_rsa"
    val idrsapub = dotSsh/"id_rsa.pub"
    val knownHosts = dotSsh/"known_hosts"
    for {
      _   <- Sync[F].delay(logger.info(s"Preparing .ssh directory '${dotSsh}"))
      _   <- dotSsh.mkdirs[F]
      _   <- idrsa.delete[F]
      _   <- idrsapub.delete[F]
      _   <- knownHosts.delete[F] >> knownHosts.writeString[F]("")
      pk  <- dev.sshKey.publicKey.toOpenSsh[F]
      sk  <- dev.sshKey.privateKey.toPEM[F]
      _   <- idrsa.writeString[F](sk)
      _   <- idrsapub.writeString[F](pk)
      _   <- idrsa.perm[F](P.OWNER_READ, P.OWNER_WRITE)
      _   <- knownHosts.perm[F](P.OWNER_READ, P.OWNER_WRITE)
      _   <- idrsapub.perm[F](P.OWNER_READ, P.OWNER_WRITE, P.GROUP_READ, P.OTHERS_READ)
    } yield ()
  }
}
