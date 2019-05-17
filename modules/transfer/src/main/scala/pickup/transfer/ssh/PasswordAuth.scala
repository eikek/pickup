package pickup.transfer.ssh

import cats.Applicative

trait PasswordAuth[F[_]] {

  def check(user: String, pass: String): F[Boolean]

}

object PasswordAuth {

  def apply[F[_]](f: (String, String) => F[Boolean]): PasswordAuth[F] =
    new PasswordAuth[F] {
      def check(u: String, p: String) = f(u, p)
    }


  def none[F[_]: Applicative] = PasswordAuth[F]((_, _) => Applicative[F].pure(false))
}
