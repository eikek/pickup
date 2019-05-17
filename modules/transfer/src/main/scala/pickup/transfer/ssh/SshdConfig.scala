package pickup.transfer.ssh

import java.nio.file.Path

case class SshdConfig[F[_]](bindHost: String
  , port: Int
  , root: Path
  , keys: KeyStore[F]
  , passAuth: PasswordAuth[F]
  , userRootDir: Boolean) {

}

object SshdConfig {

}
