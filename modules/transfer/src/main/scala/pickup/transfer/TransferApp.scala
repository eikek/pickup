package pickup.transfer

import cats.effect._
import cats.implicits._
import doobie._
import doobie.hikari._
import java.nio.file.Files
import java.util.Properties
import scala.concurrent.ExecutionContext

import com.zaxxer.hikari.util.DriverDataSource
import pickup.transfer.ops._
import pickup.transfer.store._

final class TransferApp[F[_]: ConcurrentEffect](val cfg: Config
  , xa: Transactor[F]
  , blockingEC: ExecutionContext
  , val runBackup: RunBackup[F])/*(implicit C: ContextShift[F])*/ {

  def setup = new Setup[F](xa)

  def inPeers = new InPeers[F](cfg, xa)

  def outPeers = new OutPeers[F](xa)

  def startServer: Resource[F, SshServer.Sshds[F]] =
    SshServer.start(xa)(cfg)
}

object TransferApp {
  private[this] val jdbcDriver = "org.sqlite.JDBC"

  def apply[F[_]: ConcurrentEffect](cfg: Config, blockingEC: ExecutionContext)(implicit CS: ContextShift[F], T: Timer[F]): Resource[F, TransferApp[F]] = {
    val cfg0 = cfg.toAbsolutePaths
    val dbdir = cfg0.workingDir.resolve("db").normalize.toAbsolutePath
    val jdbcUrl = s"jdbc:sqlite:${dbdir.resolve("pickup.db")}"
    for {
      _   <- Resource.liftF(Effect[F].delay(Files.createDirectories(dbdir)))
      ce  <- ExecutionContexts.fixedThreadPool[F](cfg0.db.poolSize)
      te  <- ExecutionContexts.cachedThreadPool[F]
      xa  <- HikariTransactor.newHikariTransactor[F](
        jdbcDriver,
        jdbcUrl,
        "sa", "",
        ce, te
      )
      ds  <- simpleDs(jdbcUrl)
      _   <- Resource.liftF(Migration.migrate(ds))
      rb  <- Resource.liftF(RunBackup(cfg, xa, blockingEC))
    } yield new TransferApp[F](cfg0, xa, blockingEC, rb)
  }

  private def simpleDs[F[_]: Effect](url: String): Resource[F, DriverDataSource] =
    Resource.make(Effect[F].delay(new DriverDataSource(url, jdbcDriver, new Properties, "sa", "")))(_ => ().pure[F])
}
