package io.iohk.atala.pollux.vc.jwt.revocation

import io.circe.syntax.*
import io.circe.{Json, JsonObject}
import io.iohk.atala.pollux.vc.jwt.revocation.BitStringError.EncodingError
import io.iohk.atala.pollux.vc.jwt.revocation.VCStatusList2021.Purpose.Revocation
import io.iohk.atala.pollux.vc.jwt.{Issuer, JWT, W3CCredential, W3cCredentialPayload}
import zio.IO

import java.time.Instant

object VCStatusList2021 {

  enum Purpose(val name: String):
    case Revocation extends Purpose("revocation")
    case Suspension extends Purpose("suspension")

  def generateRevocationVC(
      vcId: String,
      claimsId: String,
      jwtIssuer: Issuer,
      revocationData: BitString,
      purpose: Purpose = Revocation
  ): IO[EncodingError, JWT] = {
    for {
      encodedBitString <- revocationData.encoded
    } yield {
      val claims = JsonObject()
        .add("id", claimsId.asJson)
        .add("type", "StatusList2021".asJson)
        .add("statusPurpose", purpose.name.asJson)
        .add("encodedList", encodedBitString.asJson)
      val w3Credential = W3cCredentialPayload(
        `@context` = Set(
          "https://www.w3.org/2018/credentials/v1",
          "https://w3id.org/vc/status-list/2021/v1"
        ),
        maybeId = Some(vcId),
        `type` = Set("VerifiableCredential", "StatusList2021Credential"),
        issuer = jwtIssuer.did,
        issuanceDate = Instant.now,
        maybeExpirationDate = None,
        maybeCredentialSchema = None,
        credentialSubject = claims.asJson,
        maybeCredentialStatus = None,
        maybeRefreshService = None,
        maybeEvidence = None,
        maybeTermsOfUse = None
      )
      W3CCredential.toEncodedJwt(w3Credential, jwtIssuer)
    }
  }

}
