package pickup.admin

import cats.effect._
import io.circe._
import pickup.transfer.ssh.{PublicKey => SshPublicKey, PrivateKey => SshPrivateKey}
import pickup.transfer.data._

trait JsonCodec {

  implicit def publicKeyEncoder: Encoder[SshPublicKey] =
    Encoder.encodeString.contramap[SshPublicKey](pk => pk.toOpenSsh[IO].unsafeRunSync)

  implicit def publicKeyDecoder: Decoder[SshPublicKey] =
    Decoder.decodeString.
      map(s => s.replaceAll("\r?\n", "")).
      map(s => SshPublicKey.fromOpenSsh[IO](s).handleErrorWith(_ => SshPublicKey.fromPEM[IO](s)).unsafeRunSync)

  implicit def privateKeyEncoder: Encoder[SshPrivateKey] =
    Encoder.encodeString.contramap(_.toPEM[IO].unsafeRunSync)

  implicit def privateKeyDecoder: Decoder[SshPrivateKey] =
    Decoder.decodeString.map(s => SshPrivateKey.fromPEM[IO](s).unsafeRunSync)

  implicit def uriEncoder: Encoder[Uri] =
    Encoder.encodeString.contramap(_.asString)

  implicit def uriDecoder: Decoder[Uri] =
    Decoder.decodeString.map(s => Uri.parse(s) match {
      case Right(u) => u
      case Left(err) => sys.error(err)
    })

  implicit def passwordEncoder: Encoder[Password] =
    Encoder.encodeString.contramap(_.value)

  implicit def passwordDecoder: Decoder[Password] =
    Decoder.decodeString.map(s => Password(s))

  implicit def timerCalEncoder: Encoder[TimerCal] =
    Encoder.encodeString.contramap(_.asString)

  implicit def timerCalDecoder: Decoder[TimerCal] =
    Decoder.decodeString.map(s => TimerCal.parseTimer(s) match {
      case Right(t) => t
      case Left(err) => sys.error(err)
    })
}

object JsonCodec extends JsonCodec
