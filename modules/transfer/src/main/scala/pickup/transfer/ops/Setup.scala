package pickup.transfer.ops

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import org.log4s._

import pickup.transfer.store._

/** Operations for setting up the device.
  */
final class Setup[F[_]: Effect](xa: Transactor[F]) {
  private[this] val logger = getLogger

  def apply(password: String): F[Device] =
    installDevice(xa, Device.generate[F](password))

  def get: F[Option[Device]] =
    Device.loadEnabled.transact(xa)

  def getId: F[Option[String]] =
    Device.amendLoad(sql"SELECT device_id").query[String].option.transact(xa)


  private def installDevice(xa: Transactor[F], newDev: F[Device]): F[Device] =
    for {
      devo  <- Device.loadEnabled.transact(xa)
      d     <- devo.map(d => Sync[F].delay(logger.info(s"Device ${d.id} present")).map(_ => d)).getOrElse(storeDevice(xa, newDev))
    } yield d

  private def storeDevice(xa: Transactor[F], newDev: F[Device]): F[Device] =
    for {
      d <- newDev
      _ <- Sync[F].delay(logger.info(s"Storing initial device $d"))
      _ <- Device.store(d).run.transact(xa)
      _ <- Sync[F].delay(logger.info(s"New device info ${d.id} installed"))
    } yield d
}
