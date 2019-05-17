package pickup.transfer.ssh

import cats.effect._

import java.security.{PrivateKey => JavaPrivateKey}

case class PrivateKey(asJava: JavaPrivateKey) {

  def toPEM[F[_]: Sync]: F[String] =
    Util.toPEM(this)

}

object PrivateKey {

  def fromPEM[F[_]: Sync](pem: String): F[PrivateKey] =
    Util.pemToPrivateKey(pem)

}
