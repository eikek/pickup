package pickup.transfer.store

import cats.effect._
import doobie._
import doobie.implicits._

import pickup.transfer.ssh._

object SshKeyStore extends DoobieMeta {

  def personal[F[_]: Sync](xa: Transactor[F], defaultUser: String): KeyStore[F] =
    new KeyStore[F] {
      def findPublicKey(user: String): F[Option[PublicKey]] =
        if (user != defaultUser) Sync[F].pure(None)
        else Device.amendLoad(sql"SELECT sshkeypair").
          query[KeyPair].map(_.publicKey).
          option.
          transact(xa)

      def loadHostKey: F[KeyPair] =
        loadHostKeys[F](xa)
    }

  def remote[F[_]: Sync](xa: Transactor[F]): KeyStore[F] =
    new KeyStore[F] {
      def findPublicKey(user: String): F[Option[PublicKey]] =
         sql"SELECT public_key FROM incoming_peer WHERE peer_id = $user AND enabled = true".
          query[PublicKey].
          option.
          transact(xa)

      def loadHostKey: F[KeyPair] =
        loadHostKeys[F](xa)
    }

  private def loadHostKeys[F[_]: Sync](xa: Transactor[F]): F[KeyPair] =
    Device.amendLoad(sql"SELECT hostkeypair").
      query[KeyPair].
      unique.
      transact(xa)
}
