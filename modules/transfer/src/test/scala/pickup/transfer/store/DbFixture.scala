package pickup.transfer.store

import cats.effect._
import doobie._
import java.nio.file.Files
import java.nio.file.Paths
import scala.concurrent.ExecutionContext
import org.log4s._
import minitest._
import com.zaxxer.hikari.util.DriverDataSource
import java.util.Properties

import pickup.transfer.data.gen._

trait DbFixture {
  self: SimpleTestSuite =>

  private[this] val logger = getLogger

  implicit val cs = IO.contextShift(ExecutionContext.global)

  def withTx(code: Transactor[IO] => Unit): Unit = {
    val tmp = Paths.get(System.getProperty("java.io.tmpdir"))
    val dbfile = tmp.resolve(Gen.alphaNum(12, 13).generate())
    val url = s"jdbc:h2:${dbfile.normalize.toAbsolutePath}"
    val xa = Transactor.fromDriverManager[IO](
      "org.h2.Driver", url, "sa", ""
    )
    try {
      Migration.migrate[IO](simpleDs(url)).unsafeRunSync
      code(xa)
    } finally {
      val deleted = Files.list(tmp).
        filter(f => f.getFileName.toString.startsWith(dbfile.getFileName.toString)).
        mapToInt(f => {
          if (!Files.deleteIfExists(f)) {
            logger.warn(s"Cannot delete db file: $dbfile")
            0
          } else {
            1
          }
        }).
        sum
      logger.debug(s"Deleted $deleted db files.")
    }
  }

  def testTx(name: String)(code: Transactor[IO] => Unit) =
    test(name) { withTx(code) }

  private def simpleDs(url: String): DriverDataSource =
    new DriverDataSource(url, "org.h2.Driver", new Properties, "sa", "")

}
