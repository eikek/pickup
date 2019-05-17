package pickup.transfer.ssh

import cats.effect._

import java.security.{KeyPair => JavaKeyPair, KeyPairGenerator}

case class KeyPair(publicKey: PublicKey, privateKey: PrivateKey) {

  def asJava: JavaKeyPair =
    new JavaKeyPair(publicKey.asJava, privateKey.asJava)

  def toPEM[F[_]: Sync]: F[String] =
    Util.toPEM(this)
}

object KeyPair {

  def apply(kp: JavaKeyPair): KeyPair =
    KeyPair(PublicKey(kp.getPublic), PrivateKey(kp.getPrivate))

  def fromPEM[F[_]: Sync](pem: String): F[KeyPair] =
    Util.pemToKeyPair(pem)

  def generate[F[_]: Sync]: F[KeyPair] =
    Sync[F].delay {
      val keyGen = KeyPairGenerator.getInstance("RSA")
      keyGen.initialize(4096)
      KeyPair(keyGen.genKeyPair())
    }
}
