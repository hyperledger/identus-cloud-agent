package io.iohk.atala.pollux.vc.jwt

import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import com.nimbusds.jose.jwk.{Curve, ECKey}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import io.circe
import io.circe.*
import pdi.jwt.algorithms.JwtECDSAAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtCirce}
import java.security.*

opaque type JWT = String

object JWT {
  def apply(value: String): JWT = value

  extension (jwt: JWT) {
    def value: String = jwt
  }
}

trait Signer {
  def encode(claim: Json): JWT

  def signRaw(data: Array[Byte]): Array[Byte]

  val signatureSuiteName: String

}

trait ES256SByteArraySigner {

  private val provider = BouncyCastleProviderSingleton.getInstance

  def sign(privateKey: PrivateKey, data: Array[Byte]): Array[Byte] = {

    val signer = Signature.getInstance("SHA256withECDSA", provider)
    signer.initSign(privateKey)
    signer.update(data)
    signer.sign()
  }

  val signatureSuiteName: String = JwtAlgorithm.ES256.name

}

class ES256Signer(privateKey: PrivateKey) extends Signer with ES256SByteArraySigner {
  val algorithm: JwtECDSAAlgorithm = JwtAlgorithm.ES256
  private val provider = BouncyCastleProviderSingleton.getInstance
  Security.addProvider(provider)

  override def encode(claim: Json): JWT = JWT(JwtCirce.encode(claim, privateKey, algorithm))

  override def signRaw(data: Array[Byte]): Array[Byte] = {
    sign(privateKey, data)
  }

}

// works with java 7, 8, 11 & bouncycastle provider
// https://connect2id.com/products/nimbus-jose-jwt/jca-algorithm-support#alg-support-table
class ES256KSigner(privateKey: PrivateKey) extends Signer with ES256SByteArraySigner {
  lazy val signer: ECDSASigner = {
    val ecdsaSigner = ECDSASigner(privateKey, Curve.SECP256K1)
    val bouncyCastleProvider = BouncyCastleProviderSingleton.getInstance
    ecdsaSigner.getJCAContext.setProvider(bouncyCastleProvider)
    ecdsaSigner
  }

  override def signRaw(data: Array[Byte]): Array[Byte] = {
    sign(privateKey, data)
  }

  override def encode(claim: Json): JWT = {
    val claimSet = JWTClaimsSet.parse(claim.noSpaces)
    val signedJwt = SignedJWT(
      new JWSHeader.Builder(JWSAlgorithm.ES256K).build(),
      claimSet
    )
    signedJwt.sign(signer)
    JWT(signedJwt.serialize())
  }
}

def toJWKFormat(holderJwk: ECKey): JsonWebKey = {
  JsonWebKey(
    kty = "EC",
    crv = Some(holderJwk.getCurve.getName),
    x = Some(holderJwk.getX.toJSONString),
    y = Some(holderJwk.getY.toJSONString),
    d = Some(holderJwk.getD.toJSONString)
  )
}
