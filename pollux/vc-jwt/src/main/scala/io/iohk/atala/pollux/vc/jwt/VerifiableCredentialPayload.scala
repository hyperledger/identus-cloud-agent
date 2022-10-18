package io.iohk.atala.pollux.vc.jwt

import io.circe
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, HCursor, Json}
import pdi.jwt.{Jwt, JwtCirce}
import zio.prelude.*

import java.security.{KeyPairGenerator, PublicKey}
import java.time.{Instant, ZonedDateTime}
import scala.util.{Failure, Success, Try}

opaque type DID = String

object DID {
  def apply(value: String): DID = value

  extension (did: DID) {
    def value: String = did
  }
}

case class Issuer(did: DID, signer: Signer, publicKey: PublicKey)

case class Proof(`type`: String, other: Json)

trait Verifiable(proof: Proof)

sealed trait VerifiableCredentialPayload

case class W3cVerifiableCredentialPayload(payload: W3cCredentialPayload, proof: Proof)
  extends Verifiable(proof),
    VerifiableCredentialPayload

case class JwtVerifiableCredentialPayload(jwt: JWT) extends VerifiableCredentialPayload

//trait VerifiedCredential extends JWTVerified, W3CVerifiableCredential

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
    `@context`: IndexedSeq[String],
    `type`: IndexedSeq[String],
    maybeJti: Option[String],
    maybeNbf: Option[Instant],
    aud: IndexedSeq[String],
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

  def toW3CCredentialPayload: Validation[String, W3cCredentialPayload] =
    Validation.validateWith(
      Validation.fromOptionWith("Iss must be defined")(maybeIss),
      Validation.fromOptionWith("Nbf must be defined")(maybeNbf)
    ) { (iss, nbf) =>
      W3cCredentialPayload(
        `@context` = `@context`.distinct,
        maybeId = maybeJti,
        `type` = `type`.distinct,
        issuer = DID(iss),
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
    }
}

case class JwtVc(
    `@context`: IndexedSeq[String],
    `type`: IndexedSeq[String],
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
    aud: IndexedSeq[String],
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

case class W3cCredentialPayload(
                                 `@context`: IndexedSeq[String],
                                 maybeId: Option[String],
                                 `type`: IndexedSeq[String],
                                 issuer: DID,
                                 issuanceDate: Instant,
                                 maybeExpirationDate: Option[Instant],
                                 maybeCredentialSchema: Option[CredentialSchema],
                                 credentialSubject: Json,
                                 maybeCredentialStatus: Option[CredentialStatus],
                                 maybeRefreshService: Option[RefreshService],
                                 maybeEvidence: Option[Json],
                                 maybeTermsOfUse: Option[Json],

                                 /** Not part of W3C Credential but included to preserve in case of conversion from JWT. */
                                 aud: IndexedSeq[String] = IndexedSeq.empty
                               ) extends CredentialPayload(
  maybeSub = credentialSubject.hcursor.downField("id").as[String].toOption,
  `@context` = `@context`.distinct,
  `type` = `type`.distinct,
  maybeJti = maybeId,
  maybeNbf = Some(issuanceDate),
  aud = aud,
  maybeExp = maybeExpirationDate,
  maybeIss = Some(issuer.value),
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

      implicit val w3cCredentialPayloadEncoder: Encoder[W3cCredentialPayload] =
        (w3cCredentialPayload: W3cCredentialPayload) =>
          Json
            .obj(
              ("@context", w3cCredentialPayload.`@context`.asJson),
              ("id", w3cCredentialPayload.maybeId.asJson),
              ("type", w3cCredentialPayload.`type`.asJson),
              ("issuer", w3cCredentialPayload.issuer.value.asJson),
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

      implicit val proofEncoder: Encoder[Proof] =
        (proof: Proof) => proof.other.deepMerge(Map("type" -> proof.`type`).asJson)

      implicit val w3CVerifiableCredentialPayloadEncoder: Encoder[W3cVerifiableCredentialPayload] =
        (w3cVerifiableCredentialPayload: W3cVerifiableCredentialPayload) =>
          w3cVerifiableCredentialPayload.payload.asJson
            .deepMerge(Map("proof" -> w3cVerifiableCredentialPayload.proof).asJson)

      implicit val jwtVerifiableCredentialPayloadEncoder: Encoder[JwtVerifiableCredentialPayload] =
        (jwtVerifiableCredentialPayload: JwtVerifiableCredentialPayload) =>
          jwtVerifiableCredentialPayload.jwt.value.asJson

      implicit val verifiableCredentialPayloadEncoder: Encoder[VerifiableCredentialPayload] = {
        case (w3cVerifiableCredentialPayload: W3cVerifiableCredentialPayload) => w3cVerifiableCredentialPayload.asJson
        case (jwtVerifiableCredentialPayload: JwtVerifiableCredentialPayload) => jwtVerifiableCredentialPayload.asJson
      }

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

      implicit val w3cCredentialPayloadEncoder: Decoder[W3cCredentialPayload] =
        (c: HCursor) =>
          for {
            `@context` <- c.downField("@context").as[IndexedSeq[String]]
            maybeId <- c.downField("id").as[Option[String]]
            `type` <- c.downField("type").as[IndexedSeq[String]]
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
            W3cCredentialPayload(
              `@context` = `@context`.distinct,
              maybeId = maybeId,
              `type` = `type`.distinct,
              issuer = DID(issuer),
              issuanceDate = issuanceDate,
              maybeExpirationDate = maybeExpirationDate,
              maybeCredentialSchema = maybeCredentialSchema,
              credentialSubject = credentialSubject,
              maybeCredentialStatus = maybeCredentialStatus,
              maybeRefreshService = maybeRefreshService,
              maybeEvidence = maybeEvidence,
              maybeTermsOfUse = maybeTermsOfUse,
              aud = IndexedSeq.empty
            )
          }

      implicit val proofDecoder: Decoder[Proof] =
        (c: HCursor) =>
          for {
            `type` <- c.downField("type").as[String]
            other <- c.downField("type").delete.up.as[Json]
          } yield {
            Proof(
              `type` = `type`,
              other = other
            )
          }

      implicit val w3cVerifiableCredentialPayloadDecoder: Decoder[W3cVerifiableCredentialPayload] =
        (c: HCursor) =>
          for {
            payload <- c.as[W3cCredentialPayload]
            proof <- c.downField("proof").as[Proof]
          } yield {
            W3cVerifiableCredentialPayload(
              payload = payload,
              proof = proof
            )
          }

      implicit val jwtVerifiableCredentialPayloadDecoder: Decoder[JwtVerifiableCredentialPayload] =
        (c: HCursor) =>
          for {
            jwt <- c.as[String]
          } yield {
            JwtVerifiableCredentialPayload(
              jwt = JWT(jwt)
            )
          }

      implicit val verifiableCredentialPayloadDecoder: Decoder[VerifiableCredentialPayload] =
        jwtVerifiableCredentialPayloadDecoder.or(w3cVerifiableCredentialPayloadDecoder.asInstanceOf[Decoder[VerifiableCredentialPayload]])

      implicit val jwtVcDecoder: Decoder[JwtVc] =
        (c: HCursor) =>
          for {
            `@context` <- c.downField("@context").as[IndexedSeq[String]]
            `type` <- c.downField("type").as[IndexedSeq[String]]
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
            maybeAud <- c.downField("aud").as[Option[IndexedSeq[String]]]
            maybeExp <- c.downField("exp").as[Option[Instant]]
            maybeJti <- c.downField("jti").as[Option[String]]
          } yield {
            JwtCredentialPayload(
              maybeIss = maybeIss,
              maybeSub = maybeSub,
              vc = vc,
              maybeNbf = maybeNbf,
              aud = maybeAud.toIndexedSeq.flatten,
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

  def encodeJwt(payload: JwtCredentialPayload, issuer: Issuer): JWT = issuer.signer.encode(payload.asJson)

  def toEncodedJwt(payload: W3cCredentialPayload, issuer: Issuer): JWT =
    encodeJwt(payload.toJwtCredentialPayload, issuer)

  def decodeJwt(jwt: JWT, publicKey: PublicKey): Try[JwtCredentialPayload] = {
    JwtCirce.decodeRaw(jwt.value, publicKey).flatMap(decode[JwtCredentialPayload](_).toTry)
  }

  def validateEncodedJwt(jwt: JWT, publicKey: PublicKey): Boolean =
    JwtCirce.isValid(jwt.value, publicKey)
}
