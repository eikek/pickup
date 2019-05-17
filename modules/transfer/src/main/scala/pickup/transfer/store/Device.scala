package pickup.transfer.store

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import org.log4s._
import scodec.bits.ByteVector
import java.time._

import pickup.transfer.data.Password
import pickup.transfer.ssh.KeyPair
import DoobieMeta._

case class Device(
  id: String
    , hostKey: KeyPair
    , sshKey: KeyPair
    , password: Password
    , enabled: Boolean
    , insertion: Instant = Instant.now
)

object Device {
  private[this] val logger = getLogger
  private[this] val True = true

  def generate[F[_]: Sync](pw: String): F[Device] = {
    for {
      _    <- Sync[F].delay(logger.info("Generating new device ..."))
      id   <- Sync[F].delay(makeId(pw))
      _    <- Sync[F].delay(logger.info("Generating SSH keys ..."))
      host <- KeyPair.generate[F]
      ssh  <- KeyPair.generate[F]
      _    <- Sync[F].delay(logger.info(s"Generating device done: $id"))
    } yield Device(id, host, ssh, Password(pw), true)
  }

  def store(d: Device): Update0 =
    sql"INSERT INTO device (device_id, hostkeypair, sshkeypair, password, enabled,insertion) VALUES (${d.id}, ${d.hostKey}, ${d.sshKey}, ${d.password}, ${d.enabled}, ${d.insertion})".update

  def setPassword(id: String, pass: Password): ConnectionIO[Int] = {
    val pw = pass.value
    sql"UPDATE device SET password = $pw WHERE device_id = $id".update.run
  }

  def checkPassword(pass: Password): ConnectionIO[Option[Boolean]] =
    loadEnabled.map(_.map(_.password).map(pwDb => pass == pwDb))

  def amendLoad(frag: Fragment): Fragment =
    frag ++ sql" FROM device WHERE enabled = ${True} order by insertion desc limit 1"

  def loadEnabled: ConnectionIO[Option[Device]] =
    amendLoad(sql"SELECT device_id, hostkeypair, sshkeypair, password, enabled, insertion").query[Device].option

  private def makeId(pw: String): String =
    ByteVector.encodeUtf8(pw) match {
      case Right(bv) =>
        (1 to 50000).foldLeft(bv)((v, _) => v.digest("SHA-256")).
          toBase64.
          filter(_.isLetterOrDigit).
          grouped(6).
          mkString("-")
      case Left(exc) =>
        throw exc
    }
}
