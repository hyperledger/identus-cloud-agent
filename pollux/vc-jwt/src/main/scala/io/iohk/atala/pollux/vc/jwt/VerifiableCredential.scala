package io.iohk.atala.pollux.vc.jwt

import cats.Applicative
import cats.data.{Validated, ValidatedNel}
import cats.implicits.*
import io.circe
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, HCursor, Json}
import pdi.jwt.{Jwt, JwtCirce}

import java.security.{KeyPairGenerator, PublicKey}
import java.time.{Instant, ZonedDateTime}
import scala.util.{Failure, Success, Try}

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

case class JwtVerifiableCredential(jwt: EncodedJWT) extends VerifiableCredential

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
        maybeId = maybeJti,
        `type` = `type`.distinct,
        issuer = IssuerDID(id = iss),
        issuanceDate = nbf,
        maybeExpirationDate = maybeExp,
        maybeCredentialSchema = maybeCredentialSchema,
        credentialSubject = credentialSubject,
        maybeCredentialStatus = maybeConnectionStatus,
        maybeRefreshService = maybeRefreshService,
        maybeEvidence = maybeEvidence,
        maybeTermsOfUse = maybeTermsOfUse,
        aud = aud
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

case class W3CCredentialPayload(
    `@context`: Vector[String],
    maybeId: Option[String],
    `type`: Vector[String],
    issuer: IssuerDID,
    issuanceDate: Instant,
    maybeExpirationDate: Option[Instant],
    maybeCredentialSchema: Option[CredentialSchema],
    credentialSubject: Json,
    maybeCredentialStatus: Option[CredentialStatus],
    maybeRefreshService: Option[RefreshService],
    maybeEvidence: Option[Json],
    maybeTermsOfUse: Option[Json],

    /** Not part of W3C Credential but included to preserve in case of conversion from JWT. */
    aud: Vector[String] = Vector.empty
) extends CredentialPayload(
      maybeSub = credentialSubject.hcursor.downField("id").as[String].toOption,
      `@context` = `@context`.distinct,
      `type` = `type`.distinct,
      maybeJti = maybeId,
      maybeNbf = Some(issuanceDate),
      aud = aud,
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
              ("id", w3cCredentialPayload.maybeId.asJson),
              ("type", w3cCredentialPayload.`type`.asJson),
              ("issuer", w3cCredentialPayload.issuer.id.asJson),
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

  object Decoders {
    object Implicits {
      implicit val refreshServiceDecoder: Decoder[RefreshService] =
        (c: HCursor) =>
          for {
            id <- c.downField("id").as[String]
            `type` <- c.downField("type").as[String]
          } yield {
            RefreshService(id = id, `type` = `type`)
          }

      implicit val credentialSchemaDecoder: Decoder[CredentialSchema] =
        (c: HCursor) =>
          for {
            id <- c.downField("id").as[String]
            `type` <- c.downField("type").as[String]
          } yield {
            CredentialSchema(id = id, `type` = `type`)
          }

      implicit val credentialStatusEncoder: Decoder[CredentialStatus] =
        (c: HCursor) =>
          for {
            id <- c.downField("id").as[String]
            `type` <- c.downField("type").as[String]
          } yield {
            CredentialStatus(id = id, `type` = `type`)
          }

      implicit val w3cCredentialPayloadEncoder: Decoder[W3CCredentialPayload] =
        (c: HCursor) =>
          for {
            `@context` <- c.downField("@context").as[Vector[String]]
            maybeId <- c.downField("id").as[Option[String]]
            `type` <- c.downField("type").as[Vector[String]]
            issuer <- c.downField("issuer").as[String]
            issuanceDate <- c.downField("issuanceDate").as[Instant]
            maybeExpirationDate <- c.downField("expirationDate").as[Option[Instant]]
            maybeCredentialSchema <- c.downField("credentialSchema").as[Option[CredentialSchema]]
            credentialSubject <- c.downField("credentialSubject").as[Json]
            maybeCredentialStatus <- c.downField("credentialStatus").as[Option[CredentialStatus]]
            maybeRefreshService <- c.downField("refreshService").as[Option[RefreshService]]
            maybeEvidence <- c.downField("evidence").as[Option[Json]]
            maybeTermsOfUse <- c.downField("termsOfUse").as[Option[Json]]
          } yield {
            W3CCredentialPayload(
              `@context` = `@context`.distinct,
              maybeId = maybeId,
              `type` = `type`.distinct,
              issuer = IssuerDID(id = issuer),
              issuanceDate = issuanceDate,
              maybeExpirationDate = maybeExpirationDate,
              maybeCredentialSchema = maybeCredentialSchema,
              credentialSubject = credentialSubject,
              maybeCredentialStatus = maybeCredentialStatus,
              maybeRefreshService = maybeRefreshService,
              maybeEvidence = maybeEvidence,
              maybeTermsOfUse = maybeTermsOfUse,
              aud = Vector.empty
            )
          }

      implicit val jwtVcDecoder: Decoder[JwtVc] =
        (c: HCursor) =>
          for {
            `@context` <- c.downField("@context").as[Vector[String]]
            `type` <- c.downField("type").as[Vector[String]]
            maybeCredentialSchema <- c.downField("credentialSchema").as[Option[CredentialSchema]]
            credentialSubject <- c.downField("credentialSubject").as[Json]
            maybeCredentialStatus <- c.downField("credentialStatus").as[Option[CredentialStatus]]
            maybeRefreshService <- c.downField("refreshService").as[Option[RefreshService]]
            maybeEvidence <- c.downField("evidence").as[Option[Json]]
            maybeTermsOfUse <- c.downField("termsOfUse").as[Option[Json]]
          } yield {
            JwtVc(
              `@context` = `@context`.distinct,
              `type` = `type`.distinct,
              maybeCredentialSchema = maybeCredentialSchema,
              credentialSubject = credentialSubject,
              maybeCredentialStatus = maybeCredentialStatus,
              maybeRefreshService = maybeRefreshService,
              maybeEvidence = maybeEvidence,
              maybeTermsOfUse = maybeTermsOfUse
            )
          }

      implicit val jwtCredentialPayloadDecoder: Decoder[JwtCredentialPayload] =
        (c: HCursor) =>
          for {
            maybeIss <- c.downField("iss").as[Option[String]]
            maybeSub <- c.downField("sub").as[Option[String]]
            vc <- c.downField("vc").as[JwtVc]
            maybeNbf <- c.downField("nbf").as[Option[Instant]]
            maybeAud <- c.downField("aud").as[Option[Vector[String]]]
            maybeExp <- c.downField("exp").as[Option[Instant]]
            maybeJti <- c.downField("jti").as[Option[String]]
          } yield {
            JwtCredentialPayload(
              maybeIss = maybeIss,
              maybeSub = maybeSub,
              vc = vc,
              maybeNbf = maybeNbf,
              aud = maybeAud.orEmpty,
              maybeExp = maybeExp,
              maybeJti = maybeJti
            )
          }
    }
  }
}

object JwtVerifiableCredential {

  import VerifiedCredentialJson.Decoders.Implicits.*
  import VerifiedCredentialJson.Encoders.Implicits.*

  def encodeJwt(payload: JwtCredentialPayload, issuer: Issuer): EncodedJWT =
    EncodedJWT(jwt = issuer.signer.encode(payload.asJson))

  def toEncodedJwt(payload: W3CCredentialPayload, issuer: Issuer): EncodedJWT =
    encodeJwt(payload.toJwtCredentialPayload, issuer)

  def decodeJwt(encodedJWT: EncodedJWT, publicKey: PublicKey): Try[JwtCredentialPayload] = {
    JwtCirce.decodeRaw(encodedJWT.jwt, publicKey).flatMap(decode[JwtCredentialPayload](_).toTry)
  }

  def validateEncodedJwt(encodedJWT: EncodedJWT, publicKey: PublicKey): Boolean =
    JwtCirce.isValid(encodedJWT.jwt, publicKey)
}
