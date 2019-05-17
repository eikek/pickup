package pickup.admin

import cats.effect._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.implicits._
import fs2.Stream
import scala.concurrent.ExecutionContext

import org.http4s.server.middleware.Logger
import org.http4s.server.Router

object PickupServer {

  def stream[F[_]: ConcurrentEffect](cfg: Config, blockingEc: ExecutionContext)
    (implicit T: Timer[F], C: ContextShift[F]): Stream[F, Nothing] = {
    val app = for {
      pickupApp  <- PickupApp.create[F](cfg, blockingEc)
      sshds      <- pickupApp.transfer.startServer
      _          <- Resource.liftF(pickupApp.init)

      httpApp = Router(
        "/api/v1" -> PickupRoutes.routes[F](pickupApp, blockingEc, cfg),
        "/app/assets" -> WebjarRoutes.appRoutes[F](blockingEc, cfg),
        "/app" -> TemplateRoutes.indexRoutes[F](blockingEc, cfg)
      ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(false, false)(httpApp)

    } yield finalHttpApp


    Stream.resource(app).flatMap(httpApp =>
      BlazeServerBuilder[F]
        .bindHttp(cfg.http.bind.port, cfg.http.bind.host)
        .withHttpApp(httpApp)
        .serve
    )

  }.drain
}
