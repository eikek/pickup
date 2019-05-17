package pickup.transfer.store

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import org.log4s._

import pickup.transfer.ssh._

object SshPasswordAuth extends DoobieMeta {
  private[this] val logger = getLogger

  def apply[F[_]: Sync](xa: Transactor[F]): PasswordAuth[F] =
    PasswordAuth { (user, pass) =>
      val passDB = Device.amendLoad(sql"SELECT password").
        query[String].option.transact(xa)

      passDB.map { hashed =>
        val result = pass == hashed
        if (!result) {
          logger.warn("SSH access denied: invalid password")
        }
        result
      }
    }
}
