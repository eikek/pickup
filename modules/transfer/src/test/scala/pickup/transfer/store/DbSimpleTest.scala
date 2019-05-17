package pickup.transfer.store

import cats.effect._
import doobie.implicits._
import minitest._

object DbSimpleTest extends SimpleTestSuite with DbFixture {

  testTx("Test Migration") { xa =>
    val n = sql"SELECT count(*) FROM device".query[Int].unique.transact(xa).unsafeRunSync
    assertEquals(n, 0)
  }

  testTx("Save device") { xa =>
    val dev = Device.generate[IO]("abc").unsafeRunSync
    Device.store(dev).run.transact(xa).unsafeRunSync
    val n = sql"SELECT count(*) FROM device".query[Int].unique.transact(xa).unsafeRunSync
    assertEquals(n, 1)

    val dev2 = Device.loadEnabled.transact(xa).unsafeRunSync
    assertEquals(Some(dev), dev2)
  }

}
