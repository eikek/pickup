package pickup.transfer.ssh

import cats.effect._

import java.security.{PublicKey => JavaPublicKey}

case class PublicKey(asJava: JavaPublicKey) {

  def toPEM[F[_]: Sync]: F[String] =
    Util.toPEM(this)

  def toOpenSsh[F[_]: Sync]: F[String] =
    Sync[F].delay(Util.encodeOpenssh(this))
}

object PublicKey {

  def fromPEM[F[_]: Sync](pem: String): F[PublicKey] =
    Util.pemToPublicKey(pem)

  def fromOpenSsh[F[_]: Sync](pk: String): F[PublicKey] =
    Sync[F].delay(Util.decodeOpenssh(pk))
}
