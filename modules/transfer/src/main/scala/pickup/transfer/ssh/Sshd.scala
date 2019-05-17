package pickup.transfer.ssh

import cats.effect._
import cats.implicits._
import scala.collection.JavaConverters._
import org.log4s._
import java.nio.file.Files

import java.security.{PublicKey => JavaPublicKey}
import org.apache.sshd.common.NamedFactory
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.command.Command
import org.apache.sshd.server.scp.ScpCommandFactory
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory
import org.apache.sshd.common.file.root.{RootedFileSystem, RootedFileSystemProvider}

trait Sshd[F[_]] {

  def config: SshdConfig[F]

}

object Sshd {
  private[this] val logger = getLogger

  def startServer[F[_]: Effect](cfg: SshdConfig[F]): Resource[F, Sshd[F]] =
    Resource(start(cfg).map({ sshServer =>
      val sshd = new Sshd[F] {
        val config = cfg
      }
      logger.info(s"Starting SSH server: $cfg")
      (sshd, Sync[F].delay({
        logger.info(s"Stopping SSH server: $cfg")
        sshServer.stop()
      }))
    }))


  private def start[F[_]: Effect](cfg: SshdConfig[F]): F[SshServer] =
    Effect[F].delay {
      val sshd = SshServer.setUpDefaultServer()
      sshd.setPort(cfg.port)
      if (cfg.bindHost.nonEmpty) {
        sshd.setHost(cfg.bindHost)
      }
      sshd.setKeyPairProvider(_ => {
        val io = cfg.keys.loadHostKey.map(kp => List(kp.asJava))
        Effect[F].toIO(io).unsafeRunSync.asJava
      })
      sshd.setSubsystemFactories(List[NamedFactory[Command]](
        new SftpSubsystemFactory.Builder().build(),
      ).asJava)
      sshd.setCommandFactory(new ScpCommandFactory.Builder().build())

      sshd.setPasswordAuthenticator((user, pass, _) =>
        Effect[F].toIO(cfg.passAuth.check(user, pass)).unsafeRunSync)
      sshd.setPublickeyAuthenticator((user, key, _) => {
        key match {
          case pk: JavaPublicKey =>
            val check: F[Boolean] =
              cfg.keys.findPublicKey(user).
                flatMap(pku => pku.map(_.toPEM[F]).getOrElse(Effect[F].pure(""))).
                flatMap(pku => PublicKey(pk).toPEM[F].map(pkIn => pku != "" && pku == pkIn)).
                map(flag => {
                  logger.info(s"SSH access denied to '${cfg.bindHost}:${cfg.port}' using public key")
                  flag
                })

            Effect[F].toIO(check).unsafeRunSync
          case _ =>
            logger.info(s"SSH access denied to '${cfg.bindHost}:${cfg.port}'. Public key not recognized.")
            false
        }
      })
      Files.createDirectories(cfg.root)
      sshd.setFileSystemFactory(session => {
        val user = session.getUsername
        val root = cfg.userRootDir match {
          case false =>
            cfg.root.normalize.toAbsolutePath
          case true =>
            cfg.root.resolve(user).normalize.toAbsolutePath
        }
        Files.createDirectories(root)
        // you cannot use
        // FileSystems.xxxFilesystem(URI.create("root:file://…")),
        // because of strange checks in there…  even sshd-core impl in
        // VirtualFileSystemFactory instantiates the provider for each
        // access

        new RootedFileSystem(
          new RootedFileSystemProvider(), root, Map.empty[String, Any].asJava)
      })

      sshd.start()
      sshd
    }
}
