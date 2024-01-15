package io.iohk.atala.pollux.vc.jwt.revocation

import io.circe.syntax.*
import io.circe.{Json, JsonObject}
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.pollux.vc.jwt.revocation.VCStatusList2021.Purpose.Revocation
import io.iohk.atala.pollux.vc.jwt.revocation.VCStatusList2021Error.{DecodingError, EncodingError}
import zio.{IO, UIO, ZIO}

import java.time.Instant

class VCStatusList2021 private (val vcPayload: W3cCredentialPayload, jwtIssuer: Issuer) {

  def encoded: UIO[JWT] = ZIO.succeed(W3CCredential.toEncodedJwt(vcPayload, jwtIssuer))

  def toJsonWithEmbeddedProof: UIO[Json] = ZIO.succeed(W3CCredential.toJsonWithEmbeddedProof(vcPayload, jwtIssuer))

  // add a function that will return encoded credential with embedded proof
  // and possibly use that to store a json credential with embded proof in db instead of jwt


  def getBitString: IO[DecodingError, BitString] = {
    for {
      encodedBitString <- ZIO
        .fromOption(
          vcPayload.credentialSubject.hcursor.downField("encodedList").as[String].toOption
        )
        .mapError(_ => DecodingError("'encodedList' attribute not found in credential subject"))
      bitString <- BitString.valueOf(encodedBitString).mapError(e => DecodingError(e.message))
    } yield bitString
  }
}

object VCStatusList2021 {

  enum Purpose(val name: String):
    case Revocation extends Purpose("revocation")
    case Suspension extends Purpose("suspension")

  def build(
      vcId: String,
      slId: String,
      jwtIssuer: Issuer,
      revocationData: BitString,
      purpose: Purpose = Revocation
  ): IO[EncodingError, VCStatusList2021] = {
    for {
      encodedBitString <- revocationData.encoded.mapError(e => EncodingError(e.message))
    } yield {
      val claims = JsonObject()
        .add("id", slId.asJson)
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
      VCStatusList2021(w3Credential, jwtIssuer)
    }
  }

  def decode(encodedJwtVC: JWT, issuer: Issuer): IO[DecodingError, VCStatusList2021] = {
    for {
      jwtCredentialPayload <- ZIO
        .fromTry(JwtCredential.decodeJwt(encodedJwtVC, issuer.publicKey))
        .mapError(t => DecodingError(t.getMessage))
    } yield VCStatusList2021(jwtCredentialPayload.toW3CCredentialPayload, issuer)
  }

}

sealed trait VCStatusList2021Error

object VCStatusList2021Error {
  final case class EncodingError(msg: String) extends VCStatusList2021Error
  final case class DecodingError(msg: String) extends VCStatusList2021Error
}
