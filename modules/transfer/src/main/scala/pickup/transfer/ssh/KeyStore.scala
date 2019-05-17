package pickup.transfer.ssh

trait KeyStore[F[_]] {

  def findPublicKey(user: String): F[Option[PublicKey]]

  def loadHostKey: F[KeyPair]

}
