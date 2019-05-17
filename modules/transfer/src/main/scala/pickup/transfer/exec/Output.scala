package pickup.transfer.exec

import java.time._
import java.nio.file.{Paths, Path}
import io.circe._
import io.circe.generic.semiauto._

case class Output(runAt: Instant
  , runTime: Duration
  , returnCode: Int
  , success: Boolean
  , runCount: Int
  , runSuccess: Int
  , stdout: Path
  , stderr: Path) {

  def updateCounter(other: Output): Output =
    copy(runCount = runCount + other.runCount, runSuccess = runSuccess + other.runSuccess)

  def updateCounter(other: Option[Output]): Output =
    other.map(updateCounter).getOrElse(this)

  def failure: Boolean =
    !success
}

object Output {
  implicit def durationEncoder: Encoder[Duration] =
    Encoder.encodeString.contramap(_.toString)

  implicit def durationDecoder: Decoder[Duration] =
    Decoder.decodeString.map(s => Duration.parse(s))

  implicit def instantEncoder: Encoder[Instant] =
    Encoder.encodeString.contramap(_.toString)

  implicit def instantDecoder: Decoder[Instant] =
    Decoder.decodeString.map(s => Instant.parse(s))

  implicit def pathEncoder: Encoder[Path] =
    Encoder.encodeString.contramap(_.toAbsolutePath.normalize.toString)

  implicit def pathDecoder: Decoder[Path] =
    Decoder.decodeString.map(s => Paths.get(s))


  implicit val jsonEncoder: Encoder[Output] = deriveEncoder[Output]
  implicit val jsonDecoder: Decoder[Output] = deriveDecoder[Output]

}
