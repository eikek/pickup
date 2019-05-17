package pickup.transfer.ops

import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._

import pickup.transfer.Config
import pickup.transfer.store._
import pickup.transfer.data.Size
import pickup.transfer.data.Files._

/** Operations for incoming peers
  */
final class InPeers[F[_]: Effect](cfg: Config, xa: Transactor[F]) {

  def size(p: IncomingPeer): F[Size] =
    (cfg.remote.root/p.id).sizeDir[F]

  def loadAll: F[List[IncomingPeer]] =
    IncomingPeer.loadAll.transact(xa)

  def findPeer(id: String): F[Option[IncomingPeer]] =
    IncomingPeer.find(id).transact(xa)

  def store(p: IncomingPeer): F[Unit] =
    IncomingPeer.upsert(p).transact(xa).map(_ => ())

  def delete(id: String): F[Unit] =
    IncomingPeer.delete(id).run.transact(xa).map(_ => ())
}
