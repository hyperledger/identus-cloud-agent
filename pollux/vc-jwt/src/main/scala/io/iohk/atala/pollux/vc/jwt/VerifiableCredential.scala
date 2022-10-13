package io.iohk.atala.pollux.vc.jwt

import cats.data.{Validated, ValidatedNel}
import cats.implicits._
import cats.Applicative
import pdi.jwt.Jwt
import io.circe.generic.auto._, io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}

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
sealed trait CredentialPayload(
    maybeSub: Option[String],
    `@context`: Vector[String],
    `type`: Vector[String],
    maybeJti: Option[String],
    maybeNbf: Option[Instant],
    aud: Vector[String],
    maybeExp: Option[Instant],
    maybeIss: Option[String],
    maybeConnectionStatus: Option[CredentialStatus],
    maybeRefreshService: Option[RefreshService],
    maybeEvidence: Option[Json],
    maybeTermsOfUse: Option[Json],
    maybeCredentialSchema: Option[CredentialSchema],
    credentialSubject: Json
) {
  def toJwtCredentialPayload: JwtCredentialPayload =
    JwtCredentialPayload(
      maybeIss = maybeIss,
      maybeSub = maybeSub,
      vc = JwtVc(
        `@context` = `@context`,
        `type` = `type`,
        maybeCredentialSchema = maybeCredentialSchema,
        credentialSubject = credentialSubject,
        maybeCredentialStatus = maybeConnectionStatus,
        maybeRefreshService = maybeRefreshService,
        maybeEvidence = maybeEvidence,
        maybeTermsOfUse = maybeTermsOfUse
      ),
      maybeNbf = maybeNbf,
      aud = aud,
      maybeExp = maybeExp,
      maybeJti = maybeJti
    )

  def toW3CCredentialPayload: ValidatedNel[String, W3CCredentialPayload] =
    (
      Validated
        .cond(maybeIss.isDefined, maybeIss.get, "Iss must be defined")
        .toValidatedNel,
      Validated
        .cond(maybeNbf.isDefined, maybeNbf.get, "Nbf must be defined")
        .toValidatedNel
    ).mapN((iss, nbf) =>
      W3CCredentialPayload(
        `@context` = `@context`.distinct,
        id = maybeJti,
        `type` = `type`.distinct,
        issuer = IssuerDID(id = iss),
        issuanceDate = nbf,
        maybeExpirationDate = maybeExp,
        maybeCredentialSchema = maybeCredentialSchema,
        credentialSubject = credentialSubject,
        maybeCredentialStatus = maybeConnectionStatus,
        maybeRefreshService = maybeRefreshService,
        maybeEvidence = maybeEvidence,
        maybeTermsOfUse = maybeTermsOfUse
      )
    )
}

case class JwtVc(
    `@context`: Vector[String],
    `type`: Vector[String],
    maybeCredentialSchema: Option[CredentialSchema],
    credentialSubject: Json,
    maybeCredentialStatus: Option[CredentialStatus],
    maybeRefreshService: Option[RefreshService],
    maybeEvidence: Option[Json],
    maybeTermsOfUse: Option[Json]
)
case class JwtCredentialPayload(
    maybeIss: Option[String],
    maybeSub: Option[String],
    vc: JwtVc,
    maybeNbf: Option[Instant],
    aud: Vector[String],
    maybeExp: Option[Instant],
    maybeJti: Option[String]
) extends CredentialPayload(
      maybeSub = maybeSub.orElse(vc.credentialSubject.hcursor.downField("id").as[String].toOption),
      `@context` = vc.`@context`.distinct,
      `type` = vc.`type`.distinct,
      maybeJti = maybeJti,
      maybeNbf = maybeNbf,
      aud = aud,
      maybeExp = maybeExp,
      maybeIss = maybeIss,
      maybeConnectionStatus = vc.maybeCredentialStatus,
      maybeRefreshService = vc.maybeRefreshService,
      maybeEvidence = vc.maybeEvidence,
      maybeTermsOfUse = vc.maybeTermsOfUse,
      maybeCredentialSchema = vc.maybeCredentialSchema,
      credentialSubject = vc.credentialSubject
    )

object JwtCredentialPayload {}
case class W3CCredentialPayload(
    `@context`: Vector[String],
    id: Option[String],
    `type`: Vector[String],
    issuer: IssuerDID,
    issuanceDate: Instant,
    maybeExpirationDate: Option[Instant],
    maybeCredentialSchema: Option[CredentialSchema],
    credentialSubject: Json,
    maybeCredentialStatus: Option[CredentialStatus],
    maybeRefreshService: Option[RefreshService],
    maybeEvidence: Option[Json],
    maybeTermsOfUse: Option[Json]
) extends CredentialPayload(
      maybeSub = credentialSubject.hcursor.downField("id").as[String].toOption,
      `@context` = `@context`.distinct,
      `type` = `type`.distinct,
      maybeJti = id,
      maybeNbf = Some(issuanceDate),
      aud = Vector.empty,
      maybeExp = maybeExpirationDate,
      maybeIss = Some(issuer.id),
      maybeConnectionStatus = maybeCredentialStatus,
      maybeRefreshService = maybeRefreshService,
      maybeEvidence = maybeEvidence,
      maybeTermsOfUse = maybeTermsOfUse,
      maybeCredentialSchema = maybeCredentialSchema,
      credentialSubject = credentialSubject
    )

object VerifiedCredentialJson {
  object Encoders {
    object Implicits {
      implicit val refreshServiceEncoder: Encoder[RefreshService] =
        (refreshService: RefreshService) =>
          Json
            .obj(
              ("id", refreshService.id.asJson),
              ("type", refreshService.`type`.asJson)
            )

      implicit val credentialSchemaEncoder: Encoder[CredentialSchema] =
        (credentialSchema: CredentialSchema) =>
          Json
            .obj(
              ("id", credentialSchema.id.asJson),
              ("type", credentialSchema.`type`.asJson)
            )

      implicit val credentialStatusEncoder: Encoder[CredentialStatus] =
        (credentialStatus: CredentialStatus) =>
          Json
            .obj(
              ("id", credentialStatus.id.asJson),
              ("type", credentialStatus.`type`.asJson)
            )

      implicit val w3cCredentialPayloadEncoder: Encoder[W3CCredentialPayload] =
        (w3cCredentialPayload: W3CCredentialPayload) =>
          Json
            .obj(
              ("@context", w3cCredentialPayload.`@context`.asJson),
              ("id", w3cCredentialPayload.id.asJson),
              ("type", w3cCredentialPayload.`type`.asJson),
              ("issuer", Json.fromString(w3cCredentialPayload.issuer.id)),
              ("issuanceDate", w3cCredentialPayload.issuanceDate.asJson),
              ("expirationDate", w3cCredentialPayload.maybeExpirationDate.asJson),
              ("credentialSchema", w3cCredentialPayload.maybeCredentialSchema.asJson),
              ("credentialSubject", w3cCredentialPayload.credentialSubject),
              ("credentialStatus", w3cCredentialPayload.maybeCredentialStatus.asJson),
              ("refreshService", w3cCredentialPayload.maybeRefreshService.asJson),
              ("evidence", w3cCredentialPayload.maybeEvidence.asJson),
              ("termsOfUse", w3cCredentialPayload.maybeTermsOfUse.asJson)
            )
            .deepDropNullValues
            .dropEmptyValues

      implicit val jwtVcEncoder: Encoder[JwtVc] =
        (jwtVc: JwtVc) =>
          Json
            .obj(
              ("@context", jwtVc.`@context`.asJson),
              ("type", jwtVc.`type`.asJson),
              ("credentialSchema", jwtVc.maybeCredentialSchema.asJson),
              ("credentialSubject", jwtVc.credentialSubject),
              ("credentialStatus", jwtVc.maybeCredentialStatus.asJson),
              ("refreshService", jwtVc.maybeRefreshService.asJson),
              ("evidence", jwtVc.maybeEvidence.asJson),
              ("termsOfUse", jwtVc.maybeTermsOfUse.asJson)
            )
            .deepDropNullValues
            .dropEmptyValues

      implicit val jwtCredentialPayloadEncoder: Encoder[JwtCredentialPayload] =
        (jwtCredentialPayload: JwtCredentialPayload) =>
          Json
            .obj(
              ("iss", jwtCredentialPayload.maybeIss.asJson),
              ("sub", jwtCredentialPayload.maybeSub.asJson),
              ("vc", jwtCredentialPayload.vc.asJson),
              ("nbf", jwtCredentialPayload.maybeNbf.asJson),
              ("aud", jwtCredentialPayload.aud.asJson),
              ("exp", jwtCredentialPayload.maybeExp.asJson),
              ("jti", jwtCredentialPayload.maybeJti.asJson)
            )
            .deepDropNullValues
            .dropEmptyValues
    }
  }

}

object VerifiableCredential {
  import VerifiedCredentialJson.Encoders.Implicits._
  def createJwt(payload: JwtCredentialPayload, issuer: Issuer): JWT = {
    val jwtCredentialPayload = payload
    /* val validationResult = jwtCredentialPayload
      .map(payload => issuer.signer.encode(payload.toJson))
     */
    JWT("")
  }

  def createJsonW3CCredential(payload: W3CCredentialPayload): Json =
    payload.asJson
}
