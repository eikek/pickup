package pickup.transfer.store

import cats.effect._
import org.flywaydb.core.Flyway
import javax.sql.DataSource
import org.log4s._

object Migration {

  private[this] val logger = getLogger

  def migrate[F[_]: Sync](ds: DataSource): F[Int] =
    Sync[F].delay {
      logger.info("Running db migrations...")
      val fw = Flyway.configure().dataSource(ds).load()
      fw.repair()
      fw.migrate()
    }

}
