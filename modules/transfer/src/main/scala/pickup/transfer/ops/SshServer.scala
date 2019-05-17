package pickup.transfer.ops

import cats.effect._
import doobie._

import pickup.transfer.Config
import pickup.transfer.ssh._
import pickup.transfer.store._

object SshServer {

  def start[F[_]: Effect](xa: Transactor[F])(cfg: Config): Resource[F, Sshds[F]] = {
    val sc1 = SshdConfig(cfg.personal.host
      , cfg.personal.port
      , cfg.personal.root
      , SshKeyStore.personal(xa, cfg.personal.defaultUser)
      , SshPasswordAuth[F](xa)
      , false)
    val sc2 = SshdConfig(cfg.remote.host
      , cfg.remote.port
      , cfg.remote.root
      , SshKeyStore.remote(xa)
      , PasswordAuth.none[F]
      , true)
    for {
      sshd2 <- Sshd.startServer[F](sc2)
      sshd1 <- if (cfg.personal.enable) Sshd.startServer[F](sc1).map(Some(_)) else Resource.pure(None: Option[Sshd[F]])
    } yield Sshds[F](sshd1, sshd2)
  }


  case class Sshds[F[_]](personal: Option[Sshd[F]], remote: Sshd[F])
}
