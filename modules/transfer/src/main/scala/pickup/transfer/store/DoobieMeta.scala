package pickup.transfer.store

import cats.effect._
import doobie._

import pickup.transfer.ssh._
import pickup.transfer.data._

trait DoobieMeta {

  implicit val keypairMeta: Meta[KeyPair] =
    Meta[String].imap(s => KeyPair.fromPEM[IO](s).unsafeRunSync)(kp => kp.toPEM[IO].unsafeRunSync)

  implicit val sshPubkeyMeta: Meta[PublicKey] =
    Meta[String].imap(s => PublicKey.fromPEM[IO](s).unsafeRunSync)(pk => pk.toPEM[IO].unsafeRunSync)

  implicit val sshPrivkeyMeta: Meta[PrivateKey] =
    Meta[String].imap(s => PrivateKey.fromPEM[IO](s).unsafeRunSync)(pk => pk.toPEM[IO].unsafeRunSync)

  implicit val uriMeta: Meta[Uri] =
    Meta[String].timap(s => Uri.parse(s) match {
      case Right(u) => u
      case Left(err) => sys.error(err)
    })(_.asString)

  implicit val passwordMeta: Meta[Password] =
    Meta[String].imap(s => Password(s))(_.value)

  implicit val timercalMeta: Meta[TimerCal] =
    Meta[String].imap(s => TimerCal.parseTimer(s) match {
      case Right(t) => t
      case Left(err) => sys.error(err)
    })(_.asString)
}

object DoobieMeta extends DoobieMeta
