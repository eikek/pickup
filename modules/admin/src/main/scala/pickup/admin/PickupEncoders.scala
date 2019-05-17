package pickup.admin

import cats.effect._
import org.http4s._
import org.http4s.circe._

import pickup.admin.model._

trait PickupEncoders {

  implicit def jsonEnityEncoder[F[_]: Sync]: EntityDecoder[F, SetupData] =
    jsonOf

}
