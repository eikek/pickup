import com.github.eikek.sbt.openapi._
import scala.sys.process._
import com.typesafe.sbt.SbtGit.GitKeys._

lazy val sharedSettings = Seq(
  scalaVersion := Dependencies.ScalaVersion,
  scalacOptions ++= Seq(
    "-encoding", "UTF-8",
    "-Xfatal-warnings", // fail when there are warnings
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:higherKinds",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Ywarn-unused-import",
    "-Ypartial-unification" //Resource.map is not inferred otherwise
  ),
  scalacOptions in (Compile, console) := Seq(),
  testFrameworks += new TestFramework("minitest.runner.Framework"),
  organization := "com.github.eikek",
  licenses := Seq("GPLv3" -> url("https://spdx.org/licenses/GPL-3.0-or-later.html")),
  homepage := Some(url("https://github.com/eikek/pickup")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/eikek/pickup.git"),
      "scm:git:git@github.com:eikek/pickup.git"
    )
  )
)

val elmSettings = Seq(
  Compile/resourceGenerators += (Def.task {
    compileElm(streams.value.log
      , (Compile/baseDirectory).value
      , (Compile/resourceManaged).value
      , name.value
      , version.value)
  }).taskValue,
  watchSources += Watched.WatchSource(
    (Compile/sourceDirectory).value/"elm"
      , FileFilter.globFilter("*.elm")
      , HiddenFileFilter
  )
)

val webjarSettings = Seq(
  Compile/resourceGenerators += (Def.task {
    copyWebjarResources(Seq((sourceDirectory in Compile).value/"webjar", (Compile/resourceDirectory).value/"openapi.yml")
      , (Compile/resourceManaged).value
      , name.value
      , version.value
      , streams.value.log
    )
  }).taskValue,
  Compile/sourceGenerators += (Def.task {
    createWebjarSource(Dependencies.webjars, (Compile/sourceManaged).value)
  }).taskValue,
  Compile/unmanagedResourceDirectories ++= Seq((Compile/resourceDirectory).value.getParentFile/"templates"),
  watchSources += Watched.WatchSource(
    (Compile / sourceDirectory).value/"webjar"
      , FileFilter.globFilter("*.js") || FileFilter.globFilter("*.css")
      , HiddenFileFilter
  )
)

lazy val runSettings = Seq(
  fork in run := true,
  javaOptions in reStart ++= Seq(
    "-Dconfig.file=" + ((baseDirectory in LocalRootProject).value / "dev.conf"),
    "-Xmx96M"
  ),
  connectInput in run := true
)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  developers := List(
    Developer(
      id = "eikek",
      name = "Eike Kettner",
      url = url("https://github.com/eikek"),
      email = ""
    )
  ),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false
)

lazy val debianSettings = Seq(
  maintainer := "Eike Kettner <eike.kettner@posteo.de>",
  packageSummary := description.value,
  packageDescription := description.value,
  mappings in Universal += {
    val conf = (Compile / resourceDirectory).value / "reference.conf"
    if (!conf.exists) {
      sys.error(s"File $conf not found")
    }
    conf -> "conf/pickup.conf"
  },
  bashScriptExtraDefines += """addJava "-Dconfig.file=${app_home}/../conf/pickup.conf""""
)

val testSettings = Seq(
  libraryDependencies ++= (Dependencies.minitest ++ Dependencies.logging ++ Dependencies.h2).map(_ % "test"),
  fork in Test := true,
  javaOptions in Test ++= Seq(
    s"-Djava.io.tmpdir=${target.value}"
  )
)

lazy val transfer = project.in(file("modules/transfer")).
  enablePlugins(BuildInfoPlugin).
  settings(sharedSettings).
  settings(testSettings).
  settings(
    name := "pickup-transfer",
    description := "Transfer data from/to peers via SSH",
    libraryDependencies ++= Dependencies.fs2 ++
      Dependencies.cats ++
      Dependencies.doobie ++
      Dependencies.minaSshd ++
      Dependencies.bouncycastle ++
      Dependencies.flyway ++
      Dependencies.sqlite ++
      Dependencies.fastparse ++
      Dependencies.circe ++
      Dependencies.javaxMail ++
      Dependencies.loggingApi,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, gitHeadCommit, gitHeadCommitDate, gitUncommittedChanges, gitDescribedVersion),
    buildInfoPackage := "pickup.transfer.data",
    buildInfoOptions += BuildInfoOption.ToJson,
    buildInfoOptions += BuildInfoOption.BuildTime
  )

lazy val admin = project.in(file("modules/admin")).
  enablePlugins(BuildInfoPlugin
    , JavaServerAppPackaging
    , DebianPlugin
    , SystemdPlugin
    , OpenApiSchema).
  settings(sharedSettings).
  settings(runSettings).
  settings(webjarSettings).
  settings(elmSettings).
  settings(debianSettings).
  settings(
    name := "pickup-admin",
    description := "The admin webapp.",
    libraryDependencies ++= Dependencies.fs2 ++
      Dependencies.cats ++
      Dependencies.http4s ++
      Dependencies.circe ++
      Dependencies.pureconfig ++
      Dependencies.logging ++
      Dependencies.yamusca ++
      Dependencies.webjars,
    addCompilerPlugin(Dependencies.kindProjectorPlugin),
    addCompilerPlugin(Dependencies.betterMonadicFor),
    openapiPackage := Pkg("pickup.admin.model"),
    openapiSpec := (Compile/resourceDirectory).value/"openapi.yml",
    openapiScalaConfig := ScalaConfig().
      withJson(ScalaJson.circeSemiauto).
      addMapping(CustomMapping.forFormatType {
        case "ssh-publickey" =>
          _.copy(typeDef = TypeDef("PublicKey", Imports("pickup.transfer.ssh.PublicKey", "pickup.admin.JsonCodec._")))
        case "ssh-privatekey" =>
          _.copy(typeDef = TypeDef("PrivateKey", Imports("pickup.transfer.ssh.PrivateKey", "pickup.admin.JsonCodec._")))
        case "schedule" =>
          _.copy(typeDef = TypeDef("TimerCal", Imports("pickup.transfer.data.TimerCal", "pickup.admin.JsonCodec._")))
        case "uri" =>
          _.copy(typeDef = TypeDef("Uri", Imports("pickup.transfer.data.Uri", "pickup.admin.JsonCodec._")))
        case "password" =>
          _.copy(typeDef = TypeDef("Password", Imports("pickup.transfer.data.Password", "pickup.admin.JsonCodec._")))
      }),
    openapiTargetLanguage := Language.Scala,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, gitHeadCommit, gitHeadCommitDate, gitUncommittedChanges, gitDescribedVersion),
    buildInfoPackage := "pickup.admin"
  ).dependsOn(transfer)

lazy val root = project.in(file(".")).
  settings(sharedSettings).
  settings(
    name := "pickup"
  ).
  aggregate(transfer, admin)

def copyWebjarResources(src: Seq[File], base: File, artifact: String, version: String, logger: Logger): Seq[File] = {
  val targetDir = base/"META-INF"/"resources"/"webjars"/artifact/version
  src.flatMap { dir =>
    if (dir.isDirectory) {
      val files = (dir ** "*").filter(_.isFile).get pair Path.relativeTo(dir)
      files.map { case (f, name) =>
        val target = targetDir/name
        logger.info(s"Copy $f -> $target")
        IO.createDirectories(Seq(target.getParentFile))
        IO.copy(Seq(f -> target))
        target
      }
    } else {
      val target = targetDir/dir.name
      logger.info(s"Copy $dir -> $target")
      IO.createDirectories(Seq(target.getParentFile))
      IO.copy(Seq(dir -> target))
      Seq(target)
    }
  }
}

def compileElm(logger: Logger, wd: File, outBase: File, artifact: String, version: String): Seq[File] = {
  logger.info("Compile elm files ...")
  val target = outBase/"META-INF"/"resources"/"webjars"/artifact/version/"pickup-app.js"
  val proc = Process(Seq("elm", "make", "--output", target.toString) ++ Seq(wd/"src"/"main"/"elm"/"Main.elm").map(_.toString), Some(wd))
  val out = proc.!!
  logger.info(out)
  Seq(target)
}

def createWebjarSource(wj: Seq[ModuleID], out: File): Seq[File] = {
  val target = out/"Webjars.scala"
  val fields = wj.map(m => s"""val ${m.name.toLowerCase.filter(_ != '-')} = "/${m.name}/${m.revision}" """).mkString("\n\n")
  val content = s"""package pickup.admin
    |object Webjars {
    |$fields
    |}
    |""".stripMargin

  IO.write(target, content)
  Seq(target)
}
