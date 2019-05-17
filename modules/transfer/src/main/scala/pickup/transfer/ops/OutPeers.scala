package pickup.transfer.ops

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._

import pickup.transfer.data.gen._
import pickup.transfer.store._

/** Operations for incoming peers
  */
final class OutPeers[F[_]: Effect](xa: Transactor[F]) {
//  private[this] val logger = getLogger

  def loadAll: F[List[OutgoingPeer]] =
    OutgoingPeer.loadAll.transact(xa)

  def findPeer(uri: String): F[Option[OutgoingPeer]] =
    OutgoingPeer.find(uri).transact(xa)

  def store(p: OutgoingPeer): F[Unit] = {
    val genId =
      if (p.id.isEmpty) Gen.alphaNum(32,33)
      else Gen.unit(p.id)

    for {
      id  <- genId.make[F]
      withId = p.copy(id = id)
      _   <- OutgoingPeer.upsert(withId).transact(xa)
    } yield ()
  }

  def delete(id: String): F[Unit] =
    OutgoingPeer.delete(id).run.transact(xa).map(_ => ())
}
