package pickup.transfer.ops

import fs2.Stream
import cats.Traverse
import cats.data.Validated
import cats.effect._
import cats.effect.concurrent.Ref
import cats.implicits._
import doobie._, doobie.implicits._
import java.time._
import org.log4s._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

import pickup.transfer.{Config, MailSender}
import pickup.transfer.store._
import pickup.transfer.exec._
import RunBackup.ScheduleData

final class RunBackup[F[_]: Concurrent: ContextShift : Timer](cfg: Config
  , xa: Transactor[F]
  , execBackup: ExecBackup[F]
  , scheduled: Ref[F, Map[String, ScheduleData[F]]]
  , executing: Ref[F, Map[String, Instant]]) extends DoobieMeta {

  private[this] val logger = getLogger

  /** Execute a backup to the given peer, if not already running.
    */
  def exec(peer: OutgoingPeer, doNotify: Boolean): F[F[Option[Output]]] = {
    def runProcess: F[Option[Output]] =
      Sync[F].bracket(
        executing.update(m => m.updated(peer.id, Instant.now)))(
        _ => execBackup.exec(peer).flatMap(out => notify(out, doNotify).map(_ => out)))(
        _ => executing.update(m => m - peer.id) >> schedule(peer).map(_ => ()))

    executing.get.map(_.get(peer.id).nonEmpty).flatMap {
      case true =>
        logger.info(s"Not starting backup to ${peer.remoteUri.asString}, because a process is already running")
        (None: Option[Output]).pure[F].pure[F]
      case false =>
        Concurrent[F].start(runProcess).map(_.join)
    }
  }

  def restore(peer: OutgoingPeer, daysBack: Option[Int]): F[F[Option[Output]]] = {
    def runProcess: F[Option[Output]] =
      Sync[F].bracket(
        executing.update(m => m.updated(peer.id, Instant.now)))(
        _ => execBackup.restore(peer, daysBack))(
        _ => executing.update(m => m - peer.id) >> schedule(peer).map(_ => ()))

    executing.get.map(_.get(peer.id).nonEmpty).flatMap {
      case true =>
        logger.info(s"Not starting restore to ${peer.remoteUri.asString}, because a process is already running")
        (None: Option[Output]).pure[F].pure[F]
      case false =>
        Concurrent[F].start(runProcess).map(_.join)
    }
  }

  def runningSince(id: String): F[Option[Instant]] =
    executing.get.map(_.get(id))

  def findOutput(peer: OutgoingPeer): F[Option[Output]] =
    Sync[F].delay(execBackup.findOutput(peer))

  def findRestoreOutput(peer: OutgoingPeer): F[Option[Output]] =
    Sync[F].delay(execBackup.findRestoreOutput(peer))

  def findSchedule(peerId: String): F[Option[ScheduleData[F]]] =
    scheduled.get.map(_.get(peerId))


  def cancelSchedule(peerId: String): F[Unit] =
    findSchedule(peerId).
      flatMap(fsch => fsch.map(_.cancel).getOrElse(().pure[F]))

  def scheduleRun(peer: OutgoingPeer, when: FiniteDuration): F[ScheduleData[F]] =
    cancelSchedule(peer.id) >>
    Sync[F].delay(
      logger.info(s"Scheduling next run of ${peer.remoteUri} in $when (at ${LocalDateTime.now.plus(Duration.ofNanos(when.toNanos))})")
    ) >>
    Concurrent[F].start(Timer[F].sleep(when).flatMap(_ => exec(peer, true))).
      map(_.cancel).
      flatMap({ fu =>
        val cancel: F[Unit] =
          Sync[F].delay(logger.debug(s"Cancel current schedule for ${peer.remoteUri}")) >>
          fu >>
          scheduled.update(m => m - peer.id).map(_ => ())
        val data = ScheduleData(peer.id, cancel, LocalDateTime.now.plus(Duration.ofMillis(when.toMillis)))
        scheduled.update(m => m.updated(peer.id, data)).map(_ => data)
      })


  def schedule(peer: OutgoingPeer): F[Option[ScheduleData[F]]] =
    cancelSchedule(peer.id) >> //cancel so that empty timer strimg deactives scheduled run
    Stream.emit(peer.schedule.flatMap(t => t.nextTriggerDelay(LocalDateTime.now))).
      unNoneTerminate.
      evalMap(fd => scheduleRun(peer, fd)).
      compile.last

  def scheduleAll: F[Unit] =
    OutgoingPeer.loadWithSchedule.transact(xa).
      flatMap(list => Traverse[List].sequence(list.map(schedule))).
      map(_ => ())

  private def notify(out: Option[Output], enable: Boolean): F[Unit] =
    if (!enable || !cfg.notifyMail.enable || out.exists(_.success)) {
      val msg = s"Not notifying via mail: success=${out.exists(_.success)}; enabledForRun=${enable}; configured=${cfg.notifyMail.enable}"
      if (out.exists(_.success)) {
        Sync[F].delay(logger.debug(msg))
      } else {
        Sync[F].delay(logger.info(msg))
      }
    } else Sync[F].delay {
      logger.info("Notifying about result")
      val client = MailSender.SmtpClient(cfg.smtp)
      val msg = MailSender.Message(
        recipients = cfg.notifyMail.recipients,
        sender = cfg.smtp.sender,
        subject = "Backup failed!",
        text = """Hello,
          |
          |This is just a mail from pickup to inform you, that the
          |last backup run was not succesful!
          |
          |You can check the pickup admin interface for more details.
          |
          |Regards,
          |Pickup Transfer
          |""".stripMargin,
        listId = Some("pickup"))
      client.send(msg) match {
        case Validated.Invalid(errs) =>
          logger.error(errs.head)(s"Error notifying by mail!")
        case Validated.Valid(_) =>
          ()
      }
    }
}

object RunBackup {

  case class ScheduleData[F[_]](name: String, cancel: F[Unit], time: LocalDateTime)


  def apply[F[_]: Concurrent: ContextShift: Timer](cfg: Config
    , xa: Transactor[F]
    , blockingEC: ExecutionContext): F[RunBackup[F]] =
    for {
      ref <- Ref.of[F, Map[String, Instant]](Map.empty)
      sch <- Ref.of[F, Map[String, ScheduleData[F]]](Map.empty)
      execBackup = new ExecBackup(cfg, xa, blockingEC)
    } yield new RunBackup(cfg, xa, execBackup, sch, ref)
}
