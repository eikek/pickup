package pickup.transfer.data

import fs2.{io, Stream}
import cats.effect.{Sync, ContextShift}
import cats.implicits._
import java.net.URL
import java.nio.file.{Files => JF, Path, Paths, StandardCopyOption, OpenOption}
import java.nio.file.attribute.PosixFilePermission
import java.nio.charset.StandardCharsets
import scodec.bits.ByteVector
import scala.concurrent.ExecutionContext
import scala.collection.JavaConverters._

import Size.Implicits._

trait Files {

  implicit final class UrlOps(url: URL) {

    def name: String =
      Option(url.getPath).
        map(p => Paths.get(p).name).
        getOrElse("")

    def open[F[_]](chunkSize: Int, blockingEC: ExecutionContext)(implicit F: Sync[F], cs: ContextShift[F]): Stream[F, Byte] = {
      Stream.bracket(F.delay(url.openStream))(in => F.delay(in.close)).
        flatMap(in => io.readInputStream(F.pure(in), chunkSize, blockingEC))
    }

  }

  implicit final class PathOps(path: Path) {
    require(path != null)

    def /(child: String): Path = path.resolve(child).normalize

    def exists: Boolean =
      JF.exists(path)

    def notExists: Boolean =
      !exists

    def isDirectory: Boolean =
      exists && JF.isDirectory(path)

    def isFile: Boolean =
      exists && JF.isRegularFile(path)

    def isSymlink: Boolean =
      JF.isSymbolicLink(path)

    def parent: Option[Path] =
      Option(path.getParent)

    def name: String =
      path.getFileName.toString

    def extension: Option[String] =
      name.lastIndexOf('.') match {
        case -1 => None
        case idx => Some(name.substring(idx + 1))
      }

    def basename: String =
      extension match {
        case Some(e) =>
          val n = name
          n.substring(0, n.length - e.length)
        case None =>
          name
      }

    def findAnyFile(files: Seq[String]): Option[Path] =
      files.map(path.resolve).find(_.exists)

    def findAnyFileInSubDirs(subs: Seq[String], files: Seq[String]): Option[Path] =
      subs.map(path.resolve).
        flatMap(sub => files.map(sub.resolve)).
        find(_.exists)

    def contents[F[_]: Sync: ContextShift](blockingEC: ExecutionContext): Stream[F, Byte] =
      io.file.readAll[F](path, blockingEC, 32 * 1024)

    def lines[F[_]: Sync: ContextShift](blockingEC: ExecutionContext): Stream[F, String] =
      contents[F](blockingEC).
        through(fs2.text.utf8Decode).
        through(fs2.text.lines)

    def mkdirs[F[_]: Sync]:  F[Path] =
      Sync[F].delay(JF.createDirectories(path))

    def writeString[F[_]: Sync](str: String, opts: OpenOption*): F[Path] =
      Sync[F].delay(JF.write(path, str.getBytes(StandardCharsets.UTF_8), opts: _*))

    def perm[F[_]: Sync](p: PosixFilePermission, ps: PosixFilePermission*): F[Path] =
      Sync[F].delay(JF.setPosixFilePermissions(path, (ps.toSet + p).asJava))

    def size: Size = JF.size(path).bytes

    def sizeFile: Size =
      if (!exists || isDirectory || isSymlink) 0.bytes
      else size

    def sizeDir[F[_]: Sync]: F[Size] =
      if (!isDirectory) sizeFile.pure[F]
      else listRec[F].fold(0L.bytes)((s, p) => s + p.sizeFile).compile.lastOrError

    def list[F[_]: Sync]: Stream[F, Path] =
      Stream.bracket(Sync[F].delay(JF.list(path)))(s => Sync[F].delay(s.close)).
        flatMap(js => Stream.fromIterator(js.iterator.asScala))

    def listRec[F[_]: Sync]: Stream[F, Path] =
      if (isDirectory) list ++ Stream.eval(list.filter(p => p.isDirectory).compile.toVector).
        flatMap(v => Stream.emits(v)).
        flatMap(p => p.listRec)
      else Stream.empty

    def etag: String = {
      ByteVector.view((path.toAbsolutePath.toString + size.toBytes).getBytes).
        digest("SHA-256").
        toHex
    }

    def checkETag(tag: String): Boolean =
      etag == tag

    def isSubpathOf(parent: Path): Boolean =
      path.startsWith(parent)

    // TODO make this more robust, but this is enough at first…
    def mimeType: String =
      extension.getOrElse("").toLowerCase match {
        case "jpg" => "image/jpeg"
        case "jpeg" => "image/jpeg"
        case "png" => "image/png"
        case "gif" => "image/gif"
        case _ => "application/octet-stream"
      }

    def moveTo[F[_]: Sync](target: Path): F[Path] =
      Sync[F].delay(JF.move(path, target
        , StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING))

    def delete[F[_]: Sync]: F[Boolean] =
      Sync[F].delay(JF.deleteIfExists(path))
  }

  object TempFile {
    def create[F[_]: Sync](prefix: String, suffix: String, parent: Path): F[Path] =
      Sync[F].delay(JF.createTempFile(parent, prefix, suffix))
  }
}

object Files extends Files
