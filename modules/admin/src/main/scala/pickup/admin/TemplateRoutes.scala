package pickup.admin

import fs2._
import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.headers._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.slf4j._
import _root_.io.circe._
import _root_.io.circe.generic.semiauto._
import _root_.io.circe.syntax._
import yamusca.imports._
import yamusca.implicits._
import scala.concurrent.ExecutionContext
import java.net.URL

object TemplateRoutes {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  val `text/html` = new MediaType("text", "html")

  def indexRoutes[F[_]: Effect](blockingEc: ExecutionContext, cfg: Config)(implicit C: ContextShift[F]): HttpRoutes[F] = {
    val indexTemplate = Util.memo(loadResource("/index.html").flatMap(loadTemplate(_, blockingEc)))
    val docTemplate = Util.memo(loadResource("/doc.html").flatMap(loadTemplate(_, blockingEc)))

    val dsl = new Http4sDsl[F]{}
    import dsl._
    HttpRoutes.of[F] {
      case GET -> Root / "index.html" =>
        for {
          templ  <- indexTemplate
          resp   <- Ok(IndexData(cfg).render(templ), `Content-Type`(`text/html`))
        } yield resp
      case GET -> Root / "doc" =>
        for {
          templ  <- docTemplate
          resp   <- Ok(DocData(cfg).render(templ), `Content-Type`(`text/html`))
        } yield resp
    }
  }

  def loadResource[F[_]: Sync](name: String): F[URL] = {
    Option(getClass.getResource(name)) match {
      case None =>
        Sync[F].raiseError(new Exception("Unknown resource: "+ name))
      case Some(r) =>
        r.pure[F]
    }
  }

  def loadUrl[F[_]: Sync](url: URL, blockingEc: ExecutionContext)(implicit C: ContextShift[F]): F[String] =
    Stream.bracket(Sync[F].delay(url.openStream))(in => Sync[F].delay(in.close)).
      flatMap(in => io.readInputStream(in.pure[F], 64 * 1024, blockingEc, false)).
      through(text.utf8Decode).
      compile.fold("")(_ + _)

  def parseTemplate[F[_]: Sync](str: String): F[Template] =
    Sync[F].delay {
      mustache.parse(str) match {
        case Right(t) => t
        case Left((_, err)) => sys.error(err)
      }
    }

  def loadTemplate[F[_]: Sync](url: URL, blockingEc: ExecutionContext)(implicit C: ContextShift[F]): F[Template] = {
    loadUrl[F](url, blockingEc).flatMap(s => parseTemplate(s)).
      map(t => {
        logger.info(s"Compiled template $url")
        t
      })
  }

  case class DocData(swaggerRoot: String, openapiSpec: String)
  object DocData {

    def apply(cfg: Config): DocData =
      DocData("/app/assets" + Webjars.swaggerui, s"/app/assets/${BuildInfo.name}/${BuildInfo.version}/openapi.yml")

    implicit def yamuscaValueConverter: ValueConverter[DocData] =
      ValueConverter.deriveConverter[DocData]
  }

  case class Flags(appName: String, apiBase: String)

  object Flags {
    def apply(cfg: Config): Flags =
      Flags(cfg.http.appName, "/api/v1")

    implicit val jsonEncoder: Encoder[Flags] =
      deriveEncoder[Flags]
    implicit def yamuscaValueConverter: ValueConverter[Flags] =
      ValueConverter.deriveConverter[Flags]
  }

  case class IndexData(flags: Flags
    , cssUrls: Seq[String]
    , jsUrls: Seq[String]
    , appExtraJs: String
    , flagsJson: String)

  object IndexData {

    def apply(cfg: Config): IndexData =
      IndexData(Flags(cfg)
        , Seq(
          "/app/assets" + Webjars.semanticui + "/semantic.min.css",
          s"/app/assets/${BuildInfo.name}/${BuildInfo.version}/pickup-admin.css"
        )
        , Seq(
          "/app/assets" + Webjars.jquery + "/jquery.min.js",
          "/app/assets" + Webjars.semanticui + "/semantic.min.js",
          s"/app/assets/${BuildInfo.name}/${BuildInfo.version}/pickup-app.js"
        )
        ,
        s"/app/assets/${BuildInfo.name}/${BuildInfo.version}/pickup-admin.js"
          , Flags(cfg).asJson.spaces2 )

    implicit def yamuscaValueConverter: ValueConverter[IndexData] =
      ValueConverter.deriveConverter[IndexData]
  }
}
