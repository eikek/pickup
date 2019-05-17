package pickup.admin

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import java.nio.file.{Files, Paths, Path}
import org.slf4j._

import pickup.transfer.data.Password
import pickup.transfer.store.Device
import pickup.transfer.TransferApp

object Main extends IOApp {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  val blockingEc: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool)

  def run(args: List[String]) = {
    val cmdline = Args(args)
    cmdline.config match {
      case Some(path) =>
        logger.info(s"Using given config file: $path")
        System.setProperty("config.file", path.toString)
      case _ =>
        Option(System.getProperty("config.file")) match {
          case Some(f) if f.nonEmpty =>
            val path = Paths.get(f).toAbsolutePath.normalize
            if (!Files.exists(path)) {
              logger.info(s"Not using config file '$f' because it doesn't exist")
              System.clearProperty("config.file")
            } else {
              logger.info(s"Using config file from system properties: $f")
            }
          case _ =>
        }
    }

    val cfg = Config.default
    cmdline.setup match {
      case Some(pw) =>
        for {
          _   <- IO(println("Setting up pickup ..."))
          dev <- runSetup(cfg, pw.value)
          _   <- IO(println(s"Created device $dev"))
        } yield ExitCode.Success
      case _ =>
        PickupServer.stream[IO](cfg, blockingEc).compile.drain.as(ExitCode.Success)
    }
  }

  private def runSetup(cfg: Config, pw: String): IO[Device] =
    TransferApp[IO](cfg.transfer, blockingEc).
      use(transfer => transfer.setup(pw))

  case class Args(config: Option[Path], setup: Option[Password])

  object Args {
    def apply(args: List[String]): Args = args match {
      case c :: "setup" :: pw :: Nil =>
        Args(Some(Paths.get(c).toAbsolutePath.normalize), Some(Password(pw)))
      case "setup" :: pw :: Nil =>
        Args(None, Some(Password(pw)))
      case c :: Nil =>
        Args(Some(Paths.get(c).toAbsolutePath.normalize), None)
      case _ =>
        Args(None, None)
    }
  }
}
