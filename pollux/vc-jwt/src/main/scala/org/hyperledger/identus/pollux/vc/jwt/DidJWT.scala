package org.hyperledger.identus.pollux.vc.jwt

import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import com.nimbusds.jose.jwk.{Curve, ECKey}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import io.circe.*
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import scala.jdk.CollectionConverters.*
import zio.*

import java.security.*
import java.security.interfaces.ECPublicKey

opaque type JWT = String

object JWT {
  def apply(value: String): JWT = value

  extension (jwt: JWT) {
    def value: String = jwt
  }
}

trait Signer {
  def encode(claim: Json): JWT

  def generateProofForJson(payload: Json, pk: PublicKey): Task[Proof]

}

// works with java 7, 8, 11 & bouncycastle provider
// https://connect2id.com/products/nimbus-jose-jwt/jca-algorithm-support#alg-support-table
class ES256KSigner(privateKey: PrivateKey) extends Signer {
  lazy val signer: ECDSASigner = {
    val ecdsaSigner = ECDSASigner(privateKey, Curve.SECP256K1)
    val bouncyCastleProvider = BouncyCastleProviderSingleton.getInstance
    ecdsaSigner.getJCAContext.setProvider(bouncyCastleProvider)
    ecdsaSigner
  }

  override def generateProofForJson(payload: Json, pk: PublicKey): Task[Proof] = {
    val err = Throwable("Public key must be secp256k1 EC public key")
    pk match
      case pk: ECPublicKey =>
        getCurveName(pk).fold(ZIO.fail(err)) { curveName =>
          if curveName != "secp256k1" then ZIO.fail(Throwable(err.getMessage + s", but got $curveName"))
          else EcdsaSecp256k1Signature2019ProofGenerator.generateProof(payload, signer, pk)
        }
      case _ => ZIO.fail(err)
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

def getCurveName(publicKey: ECPublicKey): Option[String] = {
  val params = publicKey.getParams

  val maybeCurveName = ECNamedCurveTable.getNames.asScala.find {
    case name: String =>
      val spec = ECNamedCurveTable.getParameterSpec(name)
      val curveSpec =
        new ECNamedCurveSpec(spec.getName, spec.getCurve, spec.getG, spec.getN, spec.getH, spec.getSeed)
      curveSpec.getCurve.equals(params.getCurve)
    case _ => false
  }
  maybeCurveName.fold(Option.empty[String]) { case name: String => Some(name) }

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
