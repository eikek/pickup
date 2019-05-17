package pickup.transfer.exec

import java.nio.file.{Files, Path}
import java.nio.file.StandardOpenOption._
import java.io.StringWriter
import java.time._
import org.slf4j._
import scala.sys.process._
import scala.util.{Failure, Success, Try}
import io.circe._
import io.circe.syntax._
import io.circe.parser._

import pickup.transfer.data.Files._

object Exec {
  private[this] val logger = LoggerFactory.getLogger(getClass)

  val pathSep = java.io.File.pathSeparator

  def execute(id: String)(cmd: String, args: Seq[String], homeDir: Path, addEnv: Map[String, String], attached: Boolean): Output = {
    val tmpDir = Files.createDirectories(homeDir / "exec" / id)
    val out = tmpDir.resolve("stdout.txt").toAbsolutePath.normalize
    val err = tmpDir.resolve("stderr.txt").toAbsolutePath.normalize
    if (! attached) {
      Files.deleteIfExists(out)
      Files.deleteIfExists(err)
    }
    val runjson = tmpDir.resolve("run.json").toAbsolutePath.normalize
    val existingRun = readRunjson(runjson)

    logger.info(s"Executing $cmd in WD ${tmpDir}!")
    val env = Seq(
      "HOME" -> homeDir.toAbsolutePath.toString,
      "DISPLAY" -> ""
    ) ++ addEnv.toSeq
    val started = Instant.now
    val proc = Process(Seq(cmd) ++ args
      , Some(tmpDir.toFile)
      , env.toSeq: _*
    )
    val procLogger = new ProcLogger(out, err)
    val output = Try(proc ! procLogger) match {
      case Success(rc) =>
        val rcSuccess = rc == 0
        val output = Output(started
          , Duration.between(started, Instant.now)
          , rc
          , rcSuccess
          , 1
          , if (rcSuccess) 1 else 0
          , out, err).
          updateCounter(existingRun)
        if (output.success) logger.info(s"Cmd $cmd run successful: $rc")
        else logger.error(s"Cmd $cmd returned with unsuccessful return code: $rc")
        output
      case Failure(ex) =>
        val sw = new StringWriter()
        ex.printStackTrace(new java.io.PrintWriter(sw))
        procLogger.err(sw.toString)
        Output(started, Duration.between(started, Instant.now), Int.MinValue, false, 1, 0, out, err).
          updateCounter(existingRun)
    }
    if (! attached) {
      writeRunjson(output, runjson)
    }
    output
  }

  def runAppend(cmd: String, args: Seq[String], wd: Path)(out: Path): Int = {
    (Process(Seq(cmd) ++ args, Some(wd.toFile)) #>> out.toFile).!
  }

  def findOutput(homeDir: Path, id: String): Option[Output] = {
    val meta = homeDir/"exec"/id/"run.json"
    logger.debug(s"Looking for output in ${meta}: ${meta.exists}")
    if (meta.exists) readRunjson(meta)
    else None
  }

  private def readRunjson(file: Path): Option[Output] =
    if (Files.exists(file)) {
      parse(new String(Files.readAllBytes(file))).
        getOrElse(Json.Null).
        as[Output].
        map(Some(_)).
        getOrElse({
          logger.warn(s"Cannot read meta.json: $file!")
          None
        })
    } else {
      None
    }

  private def writeRunjson(out: Output, file: Path): Unit = {
    val metaData = out.asJson.noSpaces
    logger.debug(s"Write run.json '$metaData' data to $file")
    Files.write(file, metaData.getBytes, CREATE, WRITE, TRUNCATE_EXISTING)
    ()
  }

  final class ProcLogger(outFile: Path, errFile: Path) extends ProcessLogger {
    def buffer[T](f: => T): T = f
    def err(s: => String): Unit = {
      Files.write(errFile, (s + "\n").getBytes, CREATE, WRITE, APPEND)
      logger.trace(s"stderr: $s")
    }
    def out(s: => String): Unit = {
      Files.write(outFile, (s + "\n").getBytes, CREATE, WRITE, APPEND)
      ()
      //logger.debug(s)
    }
  }
}
