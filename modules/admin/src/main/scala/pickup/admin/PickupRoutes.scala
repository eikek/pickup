package pickup.admin

import cats.effect._
import cats.implicits._
import cats.Traverse
import org.http4s._
import org.http4s.headers._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.io._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.circe.CirceEntityDecoder._
import scala.concurrent.ExecutionContext
import java.time._

import pickup.transfer.data.Size
import pickup.transfer.data.Size.Implicits._
import pickup.transfer.store._
import pickup.transfer.ops._
import pickup.transfer.exec.Output
import pickup.transfer.data._
import pickup.admin.model._

object PickupRoutes {
  val `text/plain` = new MediaType("text", "plain")
  val noCache = `Cache-Control`(CacheDirective.`no-cache`())

  object DaysBackMatcher extends OptionalQueryParamDecoderMatcher[Int]("daysBack")

  def routes[F[_]: Sync](S: PickupApp[F], blockingEc: ExecutionContext, cfg: Config)(implicit C: ContextShift[F]): HttpRoutes[F] = {
    println(C)
    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "setup" =>
        for {
          id    <- S.transfer.setup.getId
          state = id.map(SetupState(true, _)).getOrElse(SetupState(false, ""))
          resp  <- Ok(state)
        } yield resp

      case req @ POST -> Root / "setup" =>
        for {
          data   <- req.as[SetupData]
          d      <- S.transfer.setup(data.password)
          resp   <- Ok(SetupState(true, d.id))
        } yield resp

      case GET -> Root / "peers" / "in" =>
        for {
          all   <- S.transfer.inPeers.loadAll
          dtos = all.map { peer =>
            S.transfer.inPeers.size(peer).map(sz => toInPeer(sz)(peer))
          }
          list  <- Traverse[List].sequence(dtos)
          resp  <- Ok(list)
        } yield resp

      case GET -> Root / "peers" / "in" / id =>
        for {
          ip    <- S.transfer.inPeers.findPeer(id)
          size  <- ip.map(S.transfer.inPeers.size).getOrElse(0.bytes.pure[F])
          resp  <- ip.map(toInPeer(size)).map(Ok(_)).getOrElse(NotFound())
        } yield resp

      case DELETE -> Root / "peers" / "in" / id =>
        for {
          _    <- S.transfer.inPeers.delete(id)
          resp <- Ok()
        } yield resp

      case req @ POST -> Root / "peers" / "in" =>
        for {
          ip   <- req.as[InPeer]
          _    <- S.transfer.inPeers.store(toIncomingPeer(ip))
          resp <- Ok()
        } yield resp

      case GET -> Root / "peers" / "out" =>
        for {
          all    <- S.transfer.outPeers.loadAll
          dto = all.map { peer =>
            val rsince = S.transfer.runBackup.runningSince(peer.id)
            val sched = S.transfer.runBackup.findSchedule(peer.id)
            for {
              rs <- rsince
              sd <- sched
            } yield toOutPeer(rs, peer.schedule, sd, peer)
          }
          list   <- Traverse[List].sequence(dto)
          resp  <- Ok(list)
        } yield resp

      case GET -> Root / "peers" / "out" / id =>
        for {
          ip     <- S.transfer.outPeers.findPeer(id)
          rsince <- S.transfer.runBackup.runningSince(id)
          sched  <- S.transfer.runBackup.findSchedule(id)
          timer = ip.flatMap(_.schedule)
          resp  <- ip.map(toOutPeer(rsince, timer, sched, _)).map(Ok(_)).getOrElse(NotFound())
        } yield resp

      case DELETE -> Root / "peers" / "out" / id =>
        for {
          _    <- S.transfer.outPeers.delete(id)
          resp <- Ok()
        } yield resp

      case req @ POST -> Root / "peers" / "out" =>
        for {
          ip   <- req.as[OutPeer]
          peer = toOutgoingPeer(ip)
          _    <- S.transfer.outPeers.store(peer)
          _    <- if (peer.enabled) S.transfer.runBackup.schedule(peer) else S.transfer.runBackup.cancelSchedule(peer.id)
          resp <- Ok()
        } yield resp

      case req @ POST -> Root / "peers" / "out" / id / "runBackup" =>
        for {
          ip   <- S.transfer.outPeers.findPeer(id)
          _    <- ip.map(p => S.transfer.runBackup.exec(p, false)).getOrElse(None.pure[F])
          resp <- Ok()
        } yield resp

      case req @ POST -> Root / "peers" / "out" / id / "runRestore" :? DaysBackMatcher(daysBack) =>
        for {
          ip   <- S.transfer.outPeers.findPeer(id)
          _    <- ip.map(p => S.transfer.runBackup.restore(p, daysBack)).getOrElse(None.pure[F])
          resp <- Ok()
        } yield resp

      case req @ GET -> Root / "peers" / "out" / id / "backuprun" / "stdout" =>
        for {
          peer <- S.transfer.outPeers.findPeer(id)
          out  <- peer.map(S.transfer.runBackup.findOutput).getOrElse(None.pure[F])
          resp <- out.map(o => StaticFile.fromFile(o.stdout.toFile, blockingEc, Some(req)).
            map(_.withHeaders(noCache)).
            getOrElseF(NotFound())).getOrElse(NotFound())
        } yield resp


      case req @ GET -> Root / "peers" / "out" / id / "backuprun" / "stderr" =>
        for {
          peer <- S.transfer.outPeers.findPeer(id)
          out  <- peer.map(S.transfer.runBackup.findOutput).getOrElse(None.pure[F])
          resp <- out.map(o => StaticFile.fromFile(o.stderr.toFile, blockingEc, Some(req)).
            map(_.withHeaders(noCache)).
            getOrElseF(NotFound())).getOrElse(NotFound())
        } yield resp

      case GET -> Root / "peers" / "out" / id / "backuprun" =>
        for {
          peer    <- S.transfer.outPeers.findPeer(id)
          optout  <- peer.map(S.transfer.runBackup.findOutput).getOrElse((None).pure[F])
          resp    <- optout.map(toOutputData).map(Ok(_)).getOrElse(NotFound())
        } yield resp

      case req @ GET -> Root / "peers" / "out" / id / "restorerun" / "stdout" =>
        for {
          peer <- S.transfer.outPeers.findPeer(id)
          out  <- peer.map(S.transfer.runBackup.findRestoreOutput).getOrElse(None.pure[F])
          resp <- out.map(o => StaticFile.fromFile(o.stdout.toFile, blockingEc, Some(req)).
            map(_.withHeaders(noCache)).
            getOrElseF(NotFound())).getOrElse(NotFound())
        } yield resp


      case req @ GET -> Root / "peers" / "out" / id / "restorerun" / "stderr" =>
        for {
          peer <- S.transfer.outPeers.findPeer(id)
          out  <- peer.map(S.transfer.runBackup.findRestoreOutput).getOrElse(None.pure[F])
          resp <- out.map(o => StaticFile.fromFile(o.stderr.toFile, blockingEc, Some(req)).
            map(_.withHeaders(noCache)).
            getOrElseF(NotFound())).getOrElse(NotFound())
        } yield resp

      case GET -> Root / "peers" / "out" / id / "restorerun" =>
        for {
          peer    <- S.transfer.outPeers.findPeer(id)
          optout  <- peer.map(S.transfer.runBackup.findRestoreOutput).getOrElse((None).pure[F])
          resp    <- optout.map(toOutputData).map(Ok(_)).getOrElse(NotFound())
        } yield resp

      case GET -> Root / "device-public" =>
        for {
          dev   <- S.transfer.setup.get
          resp  <- dev.map(toDevPublic).map(Ok(_)).getOrElse(NotFound())
        } yield resp

      case GET -> Root / "device" =>
        for {
          dev  <- S.transfer.setup.get
          resp <- dev.map(toDeviceData(cfg)).map(Ok(_)).getOrElse(NotFound())
        } yield resp
    }
  }


  private val toOutputData: Output => OutputData =
    o => OutputData(o.runAt.toString, o.runTime.toMillis, o.returnCode, o.success, o.runCount, o.runSuccess)

  private def toDeviceData(cfg: Config): Device => DeviceData = {
    val pUri = cfg.transfer.personal.connectionUri
    val sshP = SshPersonal(pUri.host, pUri.port.getOrElse(0),pUri.user.getOrElse(""),cfg.transfer.personal.enable)
    val rUri = cfg.transfer.remote.connectionUri
    val sshR = SshRemote(rUri.host, rUri.port.getOrElse(0))
    d => DeviceData(d.id, d.password, d.sshKey.publicKey, d.sshKey.privateKey, sshP, sshR, d.insertion.toString)
  }

  private def toDevPublic(dev: Device): DevicePublic =
    DevicePublic(dev.id, dev.sshKey.publicKey)

  private def toOutPeer[F[_]](runningSince: Option[Instant]
    , timer: Option[TimerCal]
    , scheduleData: Option[RunBackup.ScheduleData[F]]
    , p: OutgoingPeer): OutPeer = {
    val (nextStr, nextMillis) = scheduleData.
      map(data => (data.time.toString, Duration.between(LocalDateTime.now, data.time).toMillis)).
      getOrElse(("", 0L))
    val sd = timer.map(t => ScheduleData(t, nextStr, nextMillis))
    val lastConn = p.lastConnection.map(_.toString).getOrElse("")
    val runTime = runningSince.map(start => Duration.between(start, Instant.now).toMillis)
    OutPeer(p.id, p.remoteUri, p.description, sd, p.enabled, runTime, p.connectionCount, lastConn)
  }

  private val toOutgoingPeer: OutPeer => OutgoingPeer =
    p => OutgoingPeer(p.id, p.remoteUri, p.description, p.enabled, p.schedule.map(_.schedule))

  private def toInPeer(size: Size): IncomingPeer => InPeer =
    p => InPeer(p.id, p.description, p.publicKey, p.enabled, p.connectionCount, p.lastConnection.map(_.toString).getOrElse(""), size.toBytes, size.asString)

  private val toIncomingPeer: InPeer => IncomingPeer =
    p => IncomingPeer(p.id, p.description, p.pubkey, p.enabled)


}
