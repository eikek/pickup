package pickup.transfer.data

import cats.effect.Sync
import cats.data.Validated
import cats.implicits._

import pickup.transfer.data.gen._

final class Password(val value: String) extends AnyVal {

  def asArray: Array[Char] = value.toCharArray

  def isEmpty: Boolean = value.isEmpty

  def nonEmpty: Boolean = !isEmpty

  def asNonEmpty: Option[Password] =
    if (isEmpty) None else Some(this)

  def masked: Password = Password.masked

  def matchOther(confirm: Password): Validated[String, Password] =
    Validated.valid(this).
      ensure("The passwords do not match.") { r =>
        r == confirm
      }

  def checkNonEmpty: Validated[String, Password] =
    Validated.valid(this).
      ensure("The password must not be empty")(_.nonEmpty)

  override def toString(): String = "***"
}

object Password {

  val masked = new Password("***")

  def apply(pw: String): Password = new Password(pw)

  def apply(pw: Array[Char]): Password = new Password(pw.mkString)

  def generate[F[_]: Sync](len: Int): F[Password] =
    Gen.password(len, len+1).make.map(Password(_))
}
