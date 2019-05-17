package pickup.admin

import pureconfig._
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import pureconfig.error.CannotConvert
import pureconfig.ConvertHelpers._
import java.nio.file.{Path, Paths}

import pickup.transfer.{Config => TransferConfig, MailSender}
import pickup.transfer.data.Uri

case class Config(transfer: TransferConfig, http: Config.Http) {}

object Config {

  implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, KebabCase))

  implicit val pathConvert: ConfigReader[Path] = ConfigReader.fromString[Path](catchReadError(s =>
    if (s.isEmpty) throw new Exception("Empty path is not allowed: "+ s)
    else Paths.get(s).toAbsolutePath.normalize
  ))

  implicit val mailConvert: ConfigReader[MailSender.Mail] = ConfigReader.fromString[MailSender.Mail](catchReadError(s =>
    MailSender.Mail(s)
  ))

  implicit val uriConvert: ConfigReader[Uri] = ConfigReader.fromString[Uri](s =>
    Uri.parse(s).left.map(err => CannotConvert(s, "Uri", err))
  )

  case class Bind(host: String, port: Int)
  case class Http(appName: String, bind: Bind)

  lazy val default: Config = {
    loadConfigOrThrow[Config]("pickup")
  }

}
