package pickup.transfer.data

import cats.Traverse
import cats.data.State
import cats.effect.Sync
import cats.implicits._

object gen {

  type Gen[A] = State[Rng, A]

  object Gen {
    def apply[A](f: Rng => (Rng, A)): Gen[A] = State(f)

    def unit[A](a: A): Gen[A] = Gen(rng => (rng, a))

    def genLong: Gen[Long] = Gen(_.nextLong)

    def genInt: Gen[Int] = genLong.map(_.##)

    def positiveInt: Gen[Int] =
      genInt.map(i => if (i < 0) -(i + 1) else i)

    def boundedInt(min: Int, max: Int): Gen[Int] =
      positiveInt.map(n => n % (max - min) + min)

    def flatten[A](list: List[Gen[A]]): Gen[List[A]] =
      Traverse[List].sequence(list)

    def chars(alphabet: IndexedSeq[Char], min: Int, max: Int): Gen[List[Char]] =
      for {
        len  <- boundedInt(min, max)
        ints <- flatten(List.fill(len)(boundedInt(0, alphabet.length)))
      } yield ints.map(alphabet)

    def string(alphabet: IndexedSeq[Char], min: Int, max: Int): Gen[String] =
      chars(alphabet, min, max).map(_.mkString)

    def alphaNum(min: Int, max: Int): Gen[String] =
      string(('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9'), min, max)

    def password(min: Int, max: Int): Gen[String] = {
      require(min > 8, "Password must be >8 chars")
      val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9') ++ "_-;§$€][^!()><=&{}*?:@#|"
      string(chars, min, max)
    }
  }

  implicit class GenOps[A](val gen: Gen[A]) extends AnyVal {
    def generate(rng: Rng = Rng()): A = gen.runA(rng).value

    def make[F[_]](implicit F: Sync[F]): F[A] =
      F.delay(generate())
  }
}
