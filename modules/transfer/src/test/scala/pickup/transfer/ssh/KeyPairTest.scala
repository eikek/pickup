package pickup.transfer.ssh

import cats.effect._
import minitest._
import pickup.transfer.Resources

object KeyPairTest extends SimpleTestSuite {

  lazy val keypair = KeyPair.generate[IO].unsafeRunSync

  test("Generate keypair") {
    assertEquals(keypair.asJava.getPublic.getAlgorithm, "RSA")
    assertEquals(keypair.asJava.getPrivate.getAlgorithm, "RSA")
  }

  test("PublicKey from/to PEM") {
    val pem = keypair.publicKey.toPEM[IO].unsafeRunSync
    val pk = PublicKey.fromPEM[IO](pem).unsafeRunSync
    assertEquals(pk, keypair.publicKey)
  }

  test("PrivateKey from/to PEM") {
    val pem = keypair.privateKey.toPEM[IO].unsafeRunSync
    val pk = PrivateKey.fromPEM[IO](pem).unsafeRunSync
    assertEquals(pk, keypair.privateKey)
  }

  test("Keypair from/to PEM") {
    val pem = keypair.toPEM[IO].unsafeRunSync
    val kp = KeyPair.fromPEM[IO](pem).unsafeRunSync
    assertEquals(kp, keypair)
  }

  test("Pubkey in openssh format") {
    val rsa = PublicKey.fromPEM[IO](Resources.asString(Resources.testRsaEncPemPub)).unsafeRunSync()
    val rsaOpenssh = Util.encodeOpenssh(rsa)
    assertEquals(rsaOpenssh, Resources.asString(Resources.testRsaEncPub))

    val dsa = PublicKey.fromPEM[IO](Resources.asString(Resources.testDsaEncPemPub)).unsafeRunSync()
    val dsaOpenssh = Util.encodeOpenssh(dsa)
    assertEquals(dsaOpenssh, Resources.asString(Resources.testDsaEncPub))
  }

  test("Decode pubkey from openssh") {
    val rsa = Util.decodeOpenssh(Resources.asString(Resources.testRsaEncPub))
    assertEquals(rsa,
      PublicKey.fromPEM[IO](Resources.asString(Resources.testRsaEncPemPub)).unsafeRunSync())

    val dsa = Util.decodeOpenssh(Resources.asString(Resources.testDsaEncPub))
    assertEquals(dsa,
      PublicKey.fromPEM[IO](Resources.asString(Resources.testDsaEncPemPub)).unsafeRunSync())
  }
}
