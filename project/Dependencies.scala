import sbt._

object Dependencies {

  val ScalaVersion = "2.12.8"

  val BetterMonadicForVersion = "0.3.0"
  val BouncycastleVersion = "1.61"
  val CatsVersion = "1.6.0"
  val CatsEffectVersion = "1.3.0"
  val CirceVersion = "0.11.1"
  val DoobieVersion = "0.7.0-M5"
  val FastparseVersion = "2.1.2"
  val FlywayVersion = "5.2.4"
  val Fs2Version = "1.0.4"
  val H2Version = "1.4.199"
  val Http4sVersion = "0.20.1"
  val KindProjectorVersion = "0.9.10"
  val Log4sVersion = "1.7.0"
  val LogbackVersion = "1.2.3"
  val MinaSshdVersion = "2.2.0"
  val MiniTestVersion = "2.4.0"
  val PureConfigVersion = "0.11.0"
  val SqliteVersion = "3.27.2.1"
  val TikaVersion = "1.20"
  val javaxMailVersion = "1.6.2"
  val dnsJavaVersion = "2.1.8"


  val cats = Seq(
    "org.typelevel" %% "cats-core" % CatsVersion,
    "org.typelevel" %% "cats-effect" % CatsEffectVersion
  )

  // https://github.com/functional-streams-for-scala/fs2
  // MIT
  val fs2 = Seq(
    "co.fs2" %% "fs2-core" % Fs2Version,
    "co.fs2" %% "fs2-io" % Fs2Version
//    "co.fs2" %% "fs2-scodec" % Fs2Version
  )

  val http4s = Seq(
    "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
    "org.http4s" %% "http4s-circe"        % Http4sVersion,
    "org.http4s" %% "http4s-dsl"          % Http4sVersion,
  )

  val circe = Seq(
    "io.circe" %% "circe-generic" % CirceVersion,
    "io.circe" %% "circe-parser" % CirceVersion
  )

  // https://github.com/apache/mina-sshd
  // ASL 2.0
  val minaSshd = Seq(
    "org.apache.sshd" % "sshd-sftp" % MinaSshdVersion,
    "org.apache.sshd" % "sshd-scp" % MinaSshdVersion
  )

  // http://www.bouncycastle.org/java.html
  // MIT
  val bouncycastle = Seq(
    "org.bouncycastle" % "bcpg-jdk15on" % BouncycastleVersion,
    "org.bouncycastle" % "bcpkix-jdk15on" % BouncycastleVersion
  )

  // https://github.com/h2database/h2database
  // MPL 2.0 or EPL 1.0
  val h2 = Seq(
    "com.h2database" % "h2" % H2Version
  )

  val sqlite = Seq(
    "org.xerial" % "sqlite-jdbc" % SqliteVersion
  )

  // https://github.com/tpolecat/doobie
  // MIT
  val doobie = Seq(
    "org.tpolecat" %% "doobie-core" % DoobieVersion,
    "org.tpolecat" %% "doobie-hikari" % DoobieVersion
  )

  // https://github.com/flyway/flyway
  // ASL 2.0
  val flyway = Seq(
    "org.flywaydb" % "flyway-core" % FlywayVersion
  )

  // https://github.com/Log4s/log4s;ASL 2.0
  val loggingApi = Seq(
    "org.log4s" %% "log4s" % Log4sVersion
  )

  val logging = Seq(
    "ch.qos.logback" % "logback-classic" % LogbackVersion
  )

  // https://github.com/melrief/pureconfig
  // MPL 2.0
  val pureconfig = Seq(
    "com.github.pureconfig" %% "pureconfig" % PureConfigVersion
  )

  val tika = Seq(
    "org.apache.tika" % "tika-core" % TikaVersion
  )

  val webjars = Seq(
    "swagger-ui" -> "3.22.1",
    "Semantic-UI" -> "2.4.1",
    "jquery" -> "3.4.1"
  ).map({case (a, v) => "org.webjars" % a % v })

  val minitest = Seq(
    // https://github.com/monix/minitest
    // Apache 2.0
    "io.monix" %% "minitest" % MiniTestVersion,
    "io.monix" %% "minitest-laws" % MiniTestVersion
  )

  val yamusca = Seq(
    "com.github.eikek" %% "yamusca-core" % "0.5.1"
  )

  val fastparse = Seq(
    "com.lihaoyi" %% "fastparse" % FastparseVersion
  )

  val javaxMail = Seq(
    "javax.mail" % "javax.mail-api" % javaxMailVersion,
    "com.sun.mail" % "javax.mail" % javaxMailVersion,
    "dnsjava" % "dnsjava" % dnsJavaVersion intransitive()
  )

  val kindProjectorPlugin = "org.spire-math" %% "kind-projector" % KindProjectorVersion
  val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % BetterMonadicForVersion
}
