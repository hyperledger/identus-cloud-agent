package org.hyperledger.identus.pollux.vc.jwt.revocation

import org.hyperledger.identus.pollux.vc.jwt.{DID, ES256Signer, Issuer, JwtCredential}
import zio.test.{Spec, ZIOSpecDefault, assertTrue}
import zio.{UIO, ZIO}

import java.security.spec.ECGenParameterSpec
import java.security.{KeyPairGenerator, SecureRandom}

object VCStatusList2021Spec extends ZIOSpecDefault {

  private val VC_ID = "https://example.com/credentials/status/3"

  private def generateIssuer(): UIO[Issuer] = {
    val keyGen = KeyPairGenerator.getInstance("EC")
    val ecSpec = ECGenParameterSpec("secp256r1")
    keyGen.initialize(ecSpec, SecureRandom())
    val keyPair = keyGen.generateKeyPair()
    val privateKey = keyPair.getPrivate
    val publicKey = keyPair.getPublic
    ZIO.succeed(
      Issuer(
        did = DID("did:issuer:MDP8AsFhHzhwUvGNuYkX7T"),
        signer = ES256Signer(privateKey),
        publicKey = publicKey
      )
    )
  }

  override def spec = suite("VCStatusList2021")(
    // TODO: add test to verify the proof is valid
    test("Should generate status list VC as JSON with embedded proof") {
      for {
        issuer <- generateIssuer()
        bitString <- BitString.getInstance()
        statusList <- VCStatusList2021.build(VC_ID, s"$VC_ID#list", issuer, bitString)
        json <- statusList.toJsonWithEmbeddedProof
      } yield {
        assertTrue(json.hcursor.downField("proof").focus.isDefined)
      }
    },
    test("Generate VC contains required fields in 'credentialSubject'") {
      for {
        issuer <- generateIssuer()
        bitString <- BitString.getInstance()
        statusList <- VCStatusList2021.build(VC_ID, s"$VC_ID#list", issuer, bitString)
        encodedJwtVC <- statusList.encoded
        jwtVCPayload <- ZIO.fromTry(JwtCredential.decodeJwt(encodedJwtVC, issuer.publicKey))
        credentialSubjectKeys <- ZIO.fromOption(jwtVCPayload.credentialSubject.hcursor.keys)
      } yield {
        assertTrue(credentialSubjectKeys.toSet == Set("id", "type", "statusPurpose", "encodedList"))
      }
    },
    test("Generated VC is valid") {
      for {
        issuer <- generateIssuer()
        bitString <- BitString.getInstance()
        statusList <- VCStatusList2021.build(VC_ID, s"$VC_ID#list", issuer, bitString)
        encodedJwtVC <- statusList.encoded
        _ <- ZIO.logInfo(s"$encodedJwtVC")
        valid <- ZIO.succeed(JwtCredential.validateEncodedJwt(encodedJwtVC, issuer.publicKey))
      } yield {
        assertTrue(valid)
      }
    },
    test("Revocation state is preserved during encoding/decoding") {
      for {
        issuer <- generateIssuer()
        bitString <- BitString.getInstance()
        _ <- bitString.setRevokedInPlace(1234, true)
        statusList <- VCStatusList2021.build(VC_ID, s"$VC_ID#list", issuer, bitString)
        encodedJwtVC <- statusList.encoded
        decodedStatusList <- VCStatusList2021.decode(encodedJwtVC, issuer)
        decodedBS <- decodedStatusList.getBitString
        revokedCount <- decodedBS.revokedCount()
        isRevoked1 <- decodedBS.isRevoked(1233)
        isRevoked2 <- decodedBS.isRevoked(1234)
      } yield {
        assertTrue(revokedCount == 1) &&
        assertTrue(!isRevoked1) &&
        assertTrue(isRevoked2)
      }
    }
  )
}
