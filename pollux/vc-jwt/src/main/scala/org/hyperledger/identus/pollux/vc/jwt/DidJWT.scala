package org.hyperledger.identus.pollux.vc.jwt

import com.nimbusds.jose.{JOSEObjectType, JWSAlgorithm, JWSHeader}
import com.nimbusds.jose.crypto.{ECDSASigner, Ed25519Signer}
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import com.nimbusds.jose.jwk.{Curve, ECKey, OctetKeyPair}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import io.circe.*
import org.hyperledger.identus.shared.crypto.Ed25519KeyPair
import zio.*

import java.security.*
import java.util.Base64

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
    EcdsaJcs2019ProofGenerator.generateProof(payload, privateKey, pk)
  }

  override def encode(claim: Json): JWT = {
    val claimSet = JWTClaimsSet.parse(claim.noSpaces)
    val signedJwt = SignedJWT(
      new JWSHeader.Builder(JWSAlgorithm.ES256K).`type`(JOSEObjectType.JWT).build(),
      claimSet
    )
    signedJwt.sign(signer)
    JWT(signedJwt.serialize())
  }
}

class EdSigner(ed25519KeyPair: Ed25519KeyPair) extends Signer {
  lazy val signer: Ed25519Signer = {
    val d = java.util.Base64.getUrlEncoder.withoutPadding().encodeToString(ed25519KeyPair.privateKey.getEncoded)
    val x = java.util.Base64.getUrlEncoder.withoutPadding().encodeToString(ed25519KeyPair.publicKey.getEncoded)
    val okpJson = s"""{"kty":"OKP","crv":"Ed25519","d":"$d","x":"$x"}"""
    val octetKeyPair = OctetKeyPair.parse(okpJson)
    val ed25519Signer = Ed25519Signer(octetKeyPair)
    ed25519Signer
  }

  override def generateProofForJson(payload: Json, pk: PublicKey): Task[Proof] = {
    EddsaJcs2022ProofGenerator.generateProof(payload, ed25519KeyPair)
  }

  override def encode(claim: Json): JWT = {
    val claimSet = JWTClaimsSet.parse(claim.noSpaces)
    val signedJwt = SignedJWT(
      new JWSHeader.Builder(JWSAlgorithm.EdDSA).build(),
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
