package pickup.transfer

import java.net.URL

object Resources {

  lazy val testRsaEnc = getClass.getResource("/testrsa_enc")
  lazy val testRsaEncPub = getClass.getResource("/testrsa_enc.pub")
  lazy val testRsaEncPemPub = getClass.getResource("/testrsa_enc_pem.pub")

  lazy val testDsaEnc = getClass.getResource("/testdsa_enc")
  lazy val testDsaEncPub = getClass.getResource("/testdsa_enc.pub")
  lazy val testDsaEncPemPub = getClass.getResource("/testdsa_enc_pem.pub")

  def asString(url: URL): String = {
    scala.io.Source.fromURL(url).mkString.trim
  }

}
