package pickup.admin

import cats.effect._
import scala.concurrent.ExecutionContext

import pickup.transfer.TransferApp

trait PickupApp[F[_]] {

  def init: F[Unit]

  def transfer: TransferApp[F]

}

object PickupApp {

  def create[F[_]: ConcurrentEffect: ContextShift : Timer](cfg: Config, blockingEc: ExecutionContext): Resource[F, PickupApp[F]] =
    for {
      transfer   <- TransferApp(cfg.transfer, blockingEc)
    } yield new Impl(cfg, transfer)


  private class Impl[F[_]: ConcurrentEffect](cfg: Config, val transfer: TransferApp[F]) extends PickupApp[F] {

    def init: F[Unit] =
      transfer.runBackup.scheduleAll
  }
}
