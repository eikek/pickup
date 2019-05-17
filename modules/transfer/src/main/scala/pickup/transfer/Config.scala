package pickup.transfer

import java.nio.file.Path

import pickup.transfer.data.Uri
import Config._

case class Config(
  workingDir: Path
    , personal: SshPersonal
    , remote: SshRemote
    , db: Db
    , gpgCmd: String
    , duplicityCmd: String
    , keyscanCmd: String
    , backupArgs: Seq[String]
    , cleanupArgs: Seq[String]
    , restoreArgs: Seq[String]
    , smtp: Smtp
    , notifyMail: Notify
) {

  def toAbsolutePaths: Config =
    copy(
      workingDir = workingDir.normalize.toAbsolutePath,
      personal = personal.copy(root = personal.root.normalize.toAbsolutePath),
      remote = remote.copy(root = remote.root.normalize.toAbsolutePath)
    )
}

object Config {

  case class SshPersonal(enable: Boolean
    , root: Path
    , host: String
    , port: Int
    , defaultUser: String
    , connectionUri: Uri
  )

  case class SshRemote(root: Path
    , host: String
    , port: Int
    , connectionUri: Uri
  )

  case class Db(poolSize: Int)

  case class Notify(enable: Boolean, recipients: Seq[MailSender.Mail])

  case class Smtp(host: String
    , port: Int
    , user: String
    , password: String
    , startTls: Boolean
    , useSsl: Boolean
    , sender: MailSender.Mail) {

    def maskPassword =
      copy(password = if (password.nonEmpty) "***" else "<none>")
  }

}
