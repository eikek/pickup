package pickup.transfer.ssh

import cats.effect._
import cats.implicits._
import java.io._
import java.math.BigInteger
import java.nio.charset.StandardCharsets.UTF_8
import java.security.KeyFactory
import java.security.interfaces.{DSAPublicKey, RSAPublicKey}
import java.security.spec.{DSAPublicKeySpec, RSAPublicKeySpec}
import java.util.Base64

import org.bouncycastle.openssl.jcajce.{JcaPEMKeyConverter, JcaPEMWriter}
import org.bouncycastle.openssl.{PEMKeyPair, PEMParser}
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo

private[ssh] final object Util {

  def toPEM[F[_]: Sync](kp: KeyPair): F[String] =
    anyToPEM(kp.asJava)


  def toPEM[F[_]: Sync](pk: PublicKey): F[String] =
    anyToPEM(pk.asJava)


  def toPEM[F[_]: Sync](pk: PrivateKey): F[String] =
    anyToPEM(pk.asJava)

  private def anyToPEM[F[_]: Sync](o: Any): F[String] =
    Sync[F].delay {
      val baos = new ByteArrayOutputStream()
      val w = new JcaPEMWriter(new OutputStreamWriter(baos))
      w.writeObject(o)
      w.flush()
      w.close()
      new String(baos.toByteArray, UTF_8)
    }


  def pemToPublicKey[F[_]: Sync](pem: String): F[PublicKey] =
    pemToAny(pem).flatMap {
      case spi: SubjectPublicKeyInfo =>
        Sync[F].delay(PublicKey(new JcaPEMKeyConverter().getPublicKey(spi)))
      case _ =>
        Sync[F].raiseError(new Exception("Invalid public key info"))
    }

  def pemToPrivateKey[F[_]: Sync](pem: String): F[PrivateKey] =
    pemToKeyPair[F](pem).map(_.privateKey)

  def pemToKeyPair[F[_]: Sync](pem: String): F[KeyPair] =
    pemToAny(pem).flatMap {
      case kp: PEMKeyPair =>
        Sync[F].delay(KeyPair(new JcaPEMKeyConverter().getKeyPair(kp)))
      case x =>
        Sync[F].raiseError(new Exception(s"Invalid pem keypair info: $x"))
    }

  private def pemToAny[F[_]: Sync](pem: String): F[AnyRef] =
    Sync[F].delay(new PEMParser(new StringReader(pem)).readObject)

  def encodeOpenssh(pk: PublicKey): String =
    pk.asJava match {
      case rsa: RSAPublicKey =>
        val baos = new ByteArrayOutputStream()
        val dos = new DataOutputStream(baos)
        dos.writeInt("ssh-rsa".length)
        dos.write("ssh-rsa".getBytes)
        dos.writeInt(rsa.getPublicExponent.toByteArray.length)
        dos.write(rsa.getPublicExponent.toByteArray)
        dos.writeInt(rsa.getModulus.toByteArray.length)
        dos.write(rsa.getModulus.toByteArray)
        "ssh-rsa " + new String(Base64.getEncoder.encode(baos.toByteArray), "UTF-8")

      case dsa: DSAPublicKey =>
        val baos = new ByteArrayOutputStream()
        val dos = new DataOutputStream(baos)
        dos.writeInt("ssh-dss".length)
        dos.write("ssh-dss".getBytes)
        dos.writeInt(dsa.getParams.getP.toByteArray.length)
        dos.write(dsa.getParams.getP.toByteArray)
        dos.writeInt(dsa.getParams.getQ.toByteArray.length)
        dos.write(dsa.getParams.getQ.toByteArray)
        dos.writeInt(dsa.getParams.getG.toByteArray.length)
        dos.write(dsa.getParams.getG.toByteArray)
        dos.writeInt(dsa.getY.toByteArray.length)
        dos.write(dsa.getY.toByteArray)
        "ssh-dss " + new String(Base64.getEncoder.encode(baos.toByteArray), "UTF-8")

      case _ =>
        sys.error("Unsupported key: "+ pk.asJava)
    }

  def decodeOpenssh(key: String): PublicKey = {
    def readNext(in: DataInputStream): Array[Byte] = {
      val len = in.readInt()
      val arr = new Array[Byte](len)
      in.readFully(arr)
      arr
    }

    val parts = key.split(' ')
    parts(0) match {
      case "ssh-rsa" =>
        val din = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder.decode(parts(1))))
        val algo = readNext(din)
        val algoName = new String(algo, "UTF-8")
        if (algoName != "ssh-rsa") {
          sys.error("Uknown algo: "+ algoName)
        }
        val pe = readNext(din)
        val m = readNext(din)
        val spec = new RSAPublicKeySpec(new BigInteger(m), new BigInteger(pe))
        val kf = KeyFactory.getInstance("RSA")
        PublicKey(kf.generatePublic(spec))

      case "ssh-dss" =>
        val din = new DataInputStream(new ByteArrayInputStream(Base64.getDecoder.decode(parts(1))))
        val algo = readNext(din)
        val algoName = new String(algo, "UTF-8")
        if (algoName != "ssh-dss") {
          sys.error("Uknown algo: "+ algoName)
        }

        val p = readNext(din)
        val q = readNext(din)
        val g = readNext(din)
        val y = readNext(din)
        val spec = new DSAPublicKeySpec(new BigInteger(y), new BigInteger(p), new BigInteger(q), new BigInteger(g))
        val kf = KeyFactory.getInstance("DSA")
        PublicKey(kf.generatePublic(spec))

      case a =>
        sys.error("Uknown key: "+ a)
    }
  }
}
