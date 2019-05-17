package pickup.transfer.store

import java.time.Instant
import doobie._, doobie.implicits._

import pickup.transfer.data._
import DoobieMeta._

case class OutgoingPeer(
  id: String
    , remoteUri: Uri
    , description: String
    , enabled: Boolean
    , schedule: Option[TimerCal]
    , connectionCount: Int = 0
    , lastConnection: Option[Instant] = None
    , insertion: Instant = Instant.now
) {


  def targetUri(dev: Device): Uri =
    remoteUri.
      fallbackProtocol("sftp").
      fallbackUser(dev.id)
}

object OutgoingPeer {
  private[this] val True = true
  private val columns = fr"peer_id, remote_uri, description, enabled, schedule, connection_count, last_connection,insertion"

  def store(p: OutgoingPeer): Update0 = {
    (fr"INSERT INTO outgoing_peer (" ++
      columns ++ fr") VALUES (" ++
      sql"${p.id}, ${p.remoteUri}, ${p.description}, ${p.enabled}, ${p.schedule}, ${p.connectionCount}, ${p.lastConnection}, ${p.insertion}" ++
      fr")").
      update
  }

  def delete(id: String): Update0 =
    sql"DELETE FROM outgoing_peer WHERE peer_id = $id".update

  def update(p: OutgoingPeer): Update0 =
    sql"UPDATE outgoing_peer SET remote_uri = ${p.remoteUri}, description = ${p.description}, enabled = ${p.enabled}, schedule = ${p.schedule} WHERE peer_id = ${p.id}".
      update

  def find(id: String): ConnectionIO[Option[OutgoingPeer]] =
    (fr"SELECT" ++ columns ++ fr"FROM outgoing_peer WHERE peer_id = $id").
      query[OutgoingPeer].option

  def upsert(p: OutgoingPeer): ConnectionIO[Int] =
    for {
      exists <- find(p.id)
      n      <- exists.map(_ => update(p).run).getOrElse(store(p).run)
    } yield n


  def loadEnabled: ConnectionIO[List[OutgoingPeer]] =
    (fr"SELECT" ++ columns ++ fr"FROM outgoing_peer WHERE enabled = ${True}").
      query[OutgoingPeer].to[List]

  def loadWithSchedule: ConnectionIO[List[OutgoingPeer]] =
    (fr"SELECT" ++ columns ++ fr"FROM outgoing_peer WHERE enabled = ${True} AND schedule is not null").
      query[OutgoingPeer].to[List]

  def loadAll: ConnectionIO[List[OutgoingPeer]] =
    (fr"SELECT" ++ columns ++ fr"FROM outgoing_peer").
      query[OutgoingPeer].to[List]

}
