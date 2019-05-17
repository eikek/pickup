package pickup.transfer.store

import doobie._, doobie.implicits._
import java.time.Instant

import pickup.transfer.ssh._
import DoobieMeta._

case class IncomingPeer(
  id: String
    , description: String
    , publicKey: PublicKey
    , enabled: Boolean
    , connectionCount: Int = 0
    , lastConnection: Option[Instant] = None
    , insertion: Instant = Instant.now
)

object IncomingPeer {
  private[this] val True = true
  private val columns = fr"peer_id, description, public_key, enabled, connection_count, last_connection, insertion"

  def store(p: IncomingPeer): Update0 =
    (fr"INSERT INTO incoming_peer (" ++ columns ++
      fr") VALUES (${p.id}, ${p.description}, ${p.publicKey}, ${p.enabled}, 0, null, ${p.insertion})").
      update

  def delete(id: String): Update0 =
    sql"DELETE FROM incoming_peer WHERE peer_id = $id".update

  def update(p: IncomingPeer): Update0 =
    sql"UPDATE incoming_peer SET description = ${p.description}, public_key = ${p.publicKey}, enabled = ${p.enabled} WHERE peer_id = ${p.id}".
      update

  def find(id: String): ConnectionIO[Option[IncomingPeer]] =
    (fr"SELECT" ++ columns ++ fr"FROM incoming_peer WHERE peer_id = $id").
      query[IncomingPeer].option

  def upsert(p: IncomingPeer): ConnectionIO[Int] =
    for {
      exists <- find(p.id)
      n      <- exists.map(_ => update(p).run).getOrElse(store(p).run)
    } yield n

  def loadEnabled: ConnectionIO[List[IncomingPeer]] =
    (fr"SELECT" ++ columns ++ fr"FROM incoming_peer WHERE enabled = ${True}").
      query[IncomingPeer].to[List]

  def loadAll: ConnectionIO[List[IncomingPeer]] =
    (fr"SELECT" ++ columns ++ fr"FROM incoming_peer").
      query[IncomingPeer].to[List]
}
