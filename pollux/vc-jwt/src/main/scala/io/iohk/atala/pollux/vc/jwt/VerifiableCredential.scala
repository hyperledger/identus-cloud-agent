package io.iohk.atala.pollux.vc.jwt

import cats.data.{Validated, ValidatedNel}
import io.circe.Json
import pdi.jwt.Jwt

import java.security.{KeyPairGenerator, PublicKey}
import java.time.{Instant, ZonedDateTime}

case class Proof(`type`: String)
trait Verifiable(proof: Proof)
case class IssuerDID(id: String)
case class Issuer(did: IssuerDID, signer: Signer, publicKey: PublicKey)
trait W3CCredential(
    `@context`: Vector[String],
    `type`: Vector[String],
    issuer: IssuerDID,
    issuanceDate: ZonedDateTime,
    expirationDate: ZonedDateTime
)

trait VerifiableCredential
trait W3CVerifiableCredential extends W3CCredential, Verifiable
case class JwtVerifiableCredential(jwt: JWT) extends VerifiableCredential
trait VerifiedCredential extends JWTVerified, W3CVerifiableCredential

case class CredentialStatus(
    id: String,
    `type`: String
)

case class RefreshService(
    id: String,
    `type`: String
)

case class CredentialSchema(
    id: String,
    `type`: String
)

trait JsonPayload {
  def toJson: Json
}

case class JwtVc(
    `@context`: Vector[String],
    `type`: Vector[String],
    credentialSchema: Option[CredentialSchema],
    credentialSubject: Json,
    credentialStatus: Option[CredentialStatus],
    refreshService: Option[RefreshService],
    evidence: Option[Json],
    termsOfUse: Option[Json]
) extends JsonPayload {
  override def toJson: Json = Json.Null // TODO
}

case class JwtCredentialPayload(
    iss: Option[String],
    sub: Option[String],
    vc: JwtVc,
    nbf: Option[Instant],
    aud: Vector[String],
    exp: Option[Instant],
    jti: Option[String]
) extends CredentialPayload,
      JsonPayload {
  override def validate: ValidatedNel[String, JwtCredentialPayload] = Validated.Valid(this).toValidatedNel // TODO

  override def toJson: Json = Json.Null // TODO
}

case class W3CCredentialPayload(
    `@context`: Vector[String],
    id: Option[String],
    `type`: Vector[String],
    issuer: IssuerDID,
    issuanceDate: Instant,
    expirationDate: Option[Instant],
    credentialSchema: Option[CredentialSchema],
    credentialSubject: Json,
    credentialStatus: Option[CredentialStatus],
    refreshService: Option[RefreshService],
    evidence: Option[Json],
    termsOfUse: Option[Json]
) extends CredentialPayload,
      JsonPayload {
  override def validate: ValidatedNel[String, W3CCredentialPayload] = Validated.Valid(this).toValidatedNel // TODO
  override def toJson: Json = Json.Null // TODO
}

sealed trait CredentialPayload extends JsonPayload {
  def validate: ValidatedNel[String, CredentialPayload]
}

object CredentialPayload {

  def validate(credentialSubject: Json): Validated[String, Json] =
    Validated
      .cond(
        credentialSubject.isArray,
        credentialSubject,
        "CredentialSubject of type array not supported"
      )

  def toJwtCredentialPayload(credentialPayload: CredentialPayload): ValidatedNel[String, JwtCredentialPayload] =
    val unvalidatedCredentialSubject = credentialPayload match
      case payload: JwtCredentialPayload => payload.vc.credentialSubject
      case payload: W3CCredentialPayload => payload.credentialSubject

    validate(unvalidatedCredentialSubject).toValidatedNel
      .map(validatedCredentialSubject => {
        val maybeExtractedSub = credentialPayload match
          case payload: JwtCredentialPayload => payload.sub
          case _: W3CCredentialPayload       => Option.empty
        val maybeSub = maybeExtractedSub.orElse(validatedCredentialSubject.hcursor.downField("id").as[String].toOption)

        val `extracted@context` = credentialPayload match
          case payload: JwtCredentialPayload => payload.vc.`@context`
          case payload: W3CCredentialPayload => payload.`@context`
        val `@context` = `extracted@context`.toSet.toVector

        val extractedType = credentialPayload match
          case payload: JwtCredentialPayload => payload.vc.`type`
          case payload: W3CCredentialPayload => payload.`type`
        val `type` = extractedType.toSet.toVector

        val extractedMaybeJti = credentialPayload match
          case payload: JwtCredentialPayload => payload.jti
          case payload: W3CCredentialPayload => payload.id
        val maybeJti = extractedMaybeJti

        val extractedMaybeNbf = credentialPayload match
          case payload: JwtCredentialPayload => payload.nbf
          case payload: W3CCredentialPayload => Some(payload.issuanceDate)
        val maybeNbf = extractedMaybeNbf

        val extractedMaybeExp = credentialPayload match
          case payload: JwtCredentialPayload => payload.exp
          case payload: W3CCredentialPayload => payload.expirationDate
        val maybeExp = extractedMaybeExp

        val extractedMaybeIss = credentialPayload match
          case payload: JwtCredentialPayload => payload.iss
          case payload: W3CCredentialPayload => Some(payload.issuer.id)
        val maybeIss = extractedMaybeIss

        val extractedMaybeConnectionStatus = credentialPayload match
          case payload: JwtCredentialPayload => payload.vc.credentialStatus
          case payload: W3CCredentialPayload => payload.credentialStatus
        val maybeConnectionStatus = extractedMaybeConnectionStatus

        val extractedMaybeRefreshService = credentialPayload match
          case payload: JwtCredentialPayload => payload.vc.refreshService
          case payload: W3CCredentialPayload => payload.refreshService
        val maybeRefreshService = extractedMaybeRefreshService

        val extractedMaybeEvidence = credentialPayload match
          case payload: JwtCredentialPayload => payload.vc.evidence
          case payload: W3CCredentialPayload => payload.evidence
        val maybeEvidence = extractedMaybeEvidence

        val extractedMaybeTermsOfUse = credentialPayload match
          case payload: JwtCredentialPayload => payload.vc.termsOfUse
          case payload: W3CCredentialPayload => payload.termsOfUse
        val maybeTermsOfUse = extractedMaybeTermsOfUse

        val extractedMaybeCredentialSchema = credentialPayload match
          case payload: JwtCredentialPayload => payload.vc.credentialSchema
          case payload: W3CCredentialPayload => payload.credentialSchema
        val maybeCredentialSchema = extractedMaybeCredentialSchema

        JwtCredentialPayload(
          iss = maybeIss,
          sub = maybeSub,
          vc = JwtVc(
            `@context` = `@context`,
            `type` = `type`,
            credentialSchema = maybeCredentialSchema,
            credentialSubject = validatedCredentialSubject,
            credentialStatus = maybeConnectionStatus,
            refreshService = maybeRefreshService,
            evidence = maybeEvidence,
            termsOfUse = maybeTermsOfUse
          ),
          nbf = maybeNbf,
          aud = Vector.empty,
          exp = maybeExp,
          jti = maybeJti
        )
      })
}

object VerifiableCredential {
  def createJwt(payload: CredentialPayload, issuer: Issuer): JWT = {
    val validatedJwtCredentialPayload = CredentialPayload.toJwtCredentialPayload(payload)
    val validationResult = validatedJwtCredentialPayload
      .andThen(_.validate)
      .map(payload => issuer.signer.encode(payload.toJson))
    JWT("")
  }
}
