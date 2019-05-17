package pickup.transfer.data

import java.security.SecureRandom

trait Rng {
  def nextLong: (Rng, Long)
}

object Rng {
  private val secureRandom = new SecureRandom()
  def apply(seed: Long = secureRandom.nextLong): Rng = new Rng {
    def nextLong: (Rng, Long) = {
      val newSeed = (seed * 1103515245L + 12345) % Int.MaxValue
      val nextRng = apply(newSeed)
      (nextRng, newSeed)
    }
  }
}
