package org.hyperledger.identus.pollux.vc.jwt

import com.nimbusds.jwt.SignedJWT
import io.circe
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import org.hyperledger.identus.castor.core.model.did.{DID, VerificationRelationship}
import org.hyperledger.identus.pollux.vc.jwt.revocation.BitString
import org.hyperledger.identus.shared.crypto.KmpSecp256k1KeyOps
import org.hyperledger.identus.shared.http.UriResolver
import org.hyperledger.identus.shared.utils.Base64Utils
import pdi.jwt.*
import zio.*
import zio.prelude.*

import java.security.PublicKey
import java.time.{Clock, Instant, OffsetDateTime, ZoneId}
import java.time.temporal.TemporalAmount
import scala.util.{Failure, Try}

case class Issuer(did: DID, signer: Signer, publicKey: PublicKey)

sealed trait VerifiableCredentialPayload

case class W3cVerifiableCredentialPayload(payload: W3cCredentialPayload, proof: JwtProof)
    extends Verifiable(proof),
      VerifiableCredentialPayload

case class JwtVerifiableCredentialPayload(jwt: JWT) extends VerifiableCredentialPayload

enum StatusPurpose {
  case Revocation
  case Suspension
}

case class CredentialStatus(
    id: String,
    `type`: String,
    statusPurpose: StatusPurpose,
    statusListIndex: Int,
    statusListCredential: String
)

case class RefreshService(
    id: String,
    `type`: String
)

case class CredentialSchema(
    id: String,
    `type`: String
)

case class CredentialIssuer(
    id: String,
    `type`: String
)

sealed trait CredentialPayload {
  def maybeSub: Option[String]

  def `@context`: Set[String]

  def `type`: Set[String]

  def maybeJti: Option[String]

  def nbf: Instant

  def aud: Set[String]

  def maybeExp: Option[Instant]

  def maybeValidFrom: Option[Instant]

  def maybeValidUntil: Option[Instant]

  def issuer: String | CredentialIssuer

  def maybeCredentialStatus: Option[CredentialStatus | List[CredentialStatus]]

  def maybeRefreshService: Option[RefreshService]

  def maybeEvidence: Option[Json]

  def maybeTermsOfUse: Option[Json]

  def maybeCredentialSchema: Option[CredentialSchema | List[CredentialSchema]]

  def credentialSubject: Json

  def toJwtCredentialPayload: JwtCredentialPayload =
    JwtCredentialPayload(
      iss = issuer match {
        case string: String                     => string
        case credentialIssuer: CredentialIssuer => credentialIssuer.id
      },
      maybeSub = maybeSub,
      vc = JwtVc(
        `@context` = `@context`,
        `type` = `type`,
        maybeCredentialSchema = maybeCredentialSchema,
        credentialSubject = credentialSubject,
        maybeCredentialStatus = maybeCredentialStatus,
        maybeRefreshService = maybeRefreshService,
        maybeEvidence = maybeEvidence,
        maybeTermsOfUse = maybeTermsOfUse,
        maybeValidFrom = maybeValidFrom,
        maybeValidUntil = maybeValidUntil,
        maybeIssuer = Some(issuer),
      ),
      nbf = nbf,
      aud = aud,
      maybeExp = maybeExp,
      maybeJti = maybeJti
    )

  def toW3CCredentialPayload: W3cCredentialPayload =
    W3cCredentialPayload(
      `@context` = `@context`,
      maybeId = maybeJti,
      `type` = `type`,
      issuer = issuer,
      issuanceDate = nbf,
      maybeExpirationDate = maybeExp,
      maybeCredentialSchema = maybeCredentialSchema,
      credentialSubject = credentialSubject,
      maybeCredentialStatus = maybeCredentialStatus,
      maybeRefreshService = maybeRefreshService,
      maybeEvidence = maybeEvidence,
      maybeTermsOfUse = maybeTermsOfUse,
      aud = aud,
      maybeValidFrom = maybeValidFrom,
      maybeValidUntil = maybeValidUntil
    )
}

case class JwtVc(
    `@context`: Set[String],
    `type`: Set[String],
    maybeCredentialSchema: Option[CredentialSchema | List[CredentialSchema]],
    credentialSubject: Json,
    maybeValidFrom: Option[Instant],
    maybeValidUntil: Option[Instant],
    maybeIssuer: Option[String | CredentialIssuer],
    maybeCredentialStatus: Option[CredentialStatus | List[CredentialStatus]],
    maybeRefreshService: Option[RefreshService],
    maybeEvidence: Option[Json],
    maybeTermsOfUse: Option[Json]
)

case class JwtCredentialPayload(
    iss: String,
    override val maybeSub: Option[String],
    vc: JwtVc,
    override val nbf: Instant,
    override val aud: Set[String],
    override val maybeExp: Option[Instant],
    override val maybeJti: Option[String]
) extends CredentialPayload {
  override val `@context` = vc.`@context`
  override val `type` = vc.`type`
  override val maybeCredentialStatus = vc.maybeCredentialStatus
  override val maybeRefreshService = vc.maybeRefreshService
  override val maybeEvidence = vc.maybeEvidence
  override val maybeTermsOfUse = vc.maybeTermsOfUse
  override val maybeCredentialSchema = vc.maybeCredentialSchema
  override val credentialSubject = vc.credentialSubject
  override val maybeValidFrom = vc.maybeValidFrom
  override val maybeValidUntil = vc.maybeValidUntil
  override val issuer = vc.maybeIssuer.getOrElse(iss)
}

case class W3cCredentialPayload(
    override val `@context`: Set[String],
    override val `type`: Set[String],
    maybeId: Option[String],
    issuer: String | CredentialIssuer,
    issuanceDate: Instant,
    maybeExpirationDate: Option[Instant],
    override val maybeCredentialSchema: Option[CredentialSchema | List[CredentialSchema]],
    override val credentialSubject: Json,
    override val maybeCredentialStatus: Option[CredentialStatus | List[CredentialStatus]],
    override val maybeRefreshService: Option[RefreshService],
    override val maybeEvidence: Option[Json],
    override val maybeTermsOfUse: Option[Json],
    override val aud: Set[String] = Set.empty,
    override val maybeValidFrom: Option[Instant],
    override val maybeValidUntil: Option[Instant]
) extends CredentialPayload {
  override val maybeSub = credentialSubject.hcursor.downField("id").as[String].toOption
  override val maybeJti = maybeId
  override val nbf = issuanceDate
  override val maybeExp = maybeExpirationDate
}

object CredentialPayload {
  object Implicits {

    import InstantDecoderEncoder.*
    import JwtProof.Implicits.*

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

    implicit val credentialIssuerEncoder: Encoder[CredentialIssuer] =
      (credentialIssuer: CredentialIssuer) =>
        Json
          .obj(
            ("id", credentialIssuer.id.asJson),
            ("type", credentialIssuer.`type`.asJson)
          )

    implicit val credentialStatusPurposeEncoder: Encoder[StatusPurpose] = (a: StatusPurpose) => a.toString.asJson

    implicit val credentialStatusEncoder: Encoder[CredentialStatus] =
      (credentialStatus: CredentialStatus) =>
        Json
          .obj(
            ("id", credentialStatus.id.asJson),
            ("type", credentialStatus.`type`.asJson),
            ("statusPurpose", credentialStatus.statusPurpose.asJson),
            ("statusListIndex", credentialStatus.statusListIndex.asJson),
            ("statusListCredential", credentialStatus.statusListCredential.asJson)
          )

    implicit val credentialStatusOrListEncoder: Encoder[CredentialStatus | List[CredentialStatus]] = Encoder.instance {
      case status: CredentialStatus           => Encoder[CredentialStatus].apply(status)
      case statusList: List[CredentialStatus] => Encoder[List[CredentialStatus]].apply(statusList)
    }

    implicit val stringOrCredentialIssuerEncoder: Encoder[String | CredentialIssuer] = Encoder.instance {
      case string: String                     => Encoder[String].apply(string)
      case credentialIssuer: CredentialIssuer => Encoder[CredentialIssuer].apply(credentialIssuer)
    }

    implicit val credentialSchemaOrListEncoder: Encoder[CredentialSchema | List[CredentialSchema]] = Encoder.instance {
      case schema: CredentialSchema           => Encoder[CredentialSchema].apply(schema)
      case schemaList: List[CredentialSchema] => Encoder[List[CredentialSchema]].apply(schemaList)
    }

    implicit val w3cCredentialPayloadEncoder: Encoder[W3cCredentialPayload] =
      (w3cCredentialPayload: W3cCredentialPayload) =>
        Json
          .obj(
            ("@context", w3cCredentialPayload.`@context`.asJson),
            ("type", w3cCredentialPayload.`type`.asJson),
            ("id", w3cCredentialPayload.maybeId.asJson),
            ("issuer", w3cCredentialPayload.issuer.asJson),
            ("issuanceDate", w3cCredentialPayload.issuanceDate.asJson),
            ("expirationDate", w3cCredentialPayload.maybeExpirationDate.asJson),
            ("validFrom", w3cCredentialPayload.maybeValidFrom.asJson),
            ("validUntil", w3cCredentialPayload.maybeValidUntil.asJson),
            ("credentialSchema", w3cCredentialPayload.maybeCredentialSchema.asJson),
            ("credentialSubject", w3cCredentialPayload.credentialSubject),
            ("credentialStatus", w3cCredentialPayload.maybeCredentialStatus.asJson),
            ("refreshService", w3cCredentialPayload.maybeRefreshService.asJson),
            ("evidence", w3cCredentialPayload.maybeEvidence.asJson),
            ("termsOfUse", w3cCredentialPayload.maybeTermsOfUse.asJson),
            ("validFrom", w3cCredentialPayload.maybeValidFrom.asJson),
            ("validUntil", w3cCredentialPayload.maybeValidUntil.asJson)
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
            ("termsOfUse", jwtVc.maybeTermsOfUse.asJson),
            ("validFrom", jwtVc.maybeValidFrom.asJson),
            ("validUntil", jwtVc.maybeValidUntil.asJson),
            ("issuer", jwtVc.maybeIssuer.asJson)
          )
          .deepDropNullValues
          .dropEmptyValues

    implicit val jwtCredentialPayloadEncoder: Encoder[JwtCredentialPayload] =
      (jwtCredentialPayload: JwtCredentialPayload) =>
        Json
          .obj(
            ("iss", jwtCredentialPayload.iss.asJson),
            ("sub", jwtCredentialPayload.maybeSub.asJson),
            ("vc", jwtCredentialPayload.vc.asJson),
            ("nbf", jwtCredentialPayload.nbf.asJson),
            ("aud", jwtCredentialPayload.aud.asJson),
            ("exp", jwtCredentialPayload.maybeExp.asJson),
            ("jti", jwtCredentialPayload.maybeJti.asJson)
          )
          .deepDropNullValues
          .dropEmptyValues

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

    implicit val credentialIssuerDecoder: Decoder[CredentialIssuer] =
      (c: HCursor) =>
        for {
          id <- c.downField("id").as[String]
          `type` <- c.downField("type").as[String]
        } yield {
          CredentialIssuer(id = id, `type` = `type`)
        }

    implicit val credentialStatusPurposeDecoder: Decoder[StatusPurpose] = (c: HCursor) =>
      Decoder.decodeString(c).flatMap { str =>
        Try(StatusPurpose.valueOf(str)).toEither.leftMap { _ =>
          DecodingFailure(s"no enum value matched for $str", List(CursorOp.Field(str)))
        }
      }

    implicit val credentialStatusDecoder: Decoder[CredentialStatus] =
      (c: HCursor) =>
        for {
          id <- c.downField("id").as[String]
          `type` <- c.downField("type").as[String]
          statusPurpose <- c.downField("statusPurpose").as[StatusPurpose]
          statusListIndex <- c.downField("statusListIndex").as[Int]
          statusListCredential <- c.downField("statusListCredential").as[String]
        } yield {
          CredentialStatus(
            id = id,
            `type` = `type`,
            statusPurpose = statusPurpose,
            statusListIndex = statusListIndex,
            statusListCredential = statusListCredential
          )
        }

    implicit val stringOrCredentialIssuerDecoder: Decoder[String | CredentialIssuer] =
      Decoder[String]
        .map(schema => schema: String | CredentialIssuer)
        .or(Decoder[CredentialIssuer].map(schema => schema: String | CredentialIssuer))

    implicit val credentialSchemaOrListDecoder: Decoder[CredentialSchema | List[CredentialSchema]] =
      Decoder[CredentialSchema]
        .map(schema => schema: CredentialSchema | List[CredentialSchema])
        .or(Decoder[List[CredentialSchema]].map(schema => schema: CredentialSchema | List[CredentialSchema]))

    implicit val credentialStatusOrListDecoder: Decoder[CredentialStatus | List[CredentialStatus]] =
      Decoder[CredentialStatus]
        .map(status => status: CredentialStatus | List[CredentialStatus])
        .or(Decoder[List[CredentialStatus]].map(status => status: CredentialStatus | List[CredentialStatus]))

    implicit val w3cCredentialPayloadDecoder: Decoder[W3cCredentialPayload] =
      (c: HCursor) =>
        for {
          `@context` <- c
            .downField("@context")
            .as[Set[String]]
            .orElse(c.downField("@context").as[String].map(Set(_)))
          `type` <- c
            .downField("type")
            .as[Set[String]]
            .orElse(c.downField("type").as[String].map(Set(_)))
          maybeId <- c.downField("id").as[Option[String]]
          issuer <- c.downField("issuer").as[String | CredentialIssuer]
          issuanceDate <- c.downField("issuanceDate").as[Instant]
          maybeExpirationDate <- c.downField("expirationDate").as[Option[Instant]]
          maybeValidFrom <- c.downField("validFrom").as[Option[Instant]]
          maybeValidUntil <- c.downField("validUntil").as[Option[Instant]]
          maybeCredentialSchema <- c
            .downField("credentialSchema")
            .as[Option[CredentialSchema | List[CredentialSchema]]]
          credentialSubject <- c.downField("credentialSubject").as[Json]
          maybeCredentialStatus <- c.downField("credentialStatus").as[Option[CredentialStatus | List[CredentialStatus]]]
          maybeRefreshService <- c.downField("refreshService").as[Option[RefreshService]]
          maybeEvidence <- c.downField("evidence").as[Option[Json]]
          maybeTermsOfUse <- c.downField("termsOfUse").as[Option[Json]]
        } yield {
          W3cCredentialPayload(
            `@context` = `@context`,
            `type` = `type`,
            maybeId = maybeId,
            issuer = issuer,
            issuanceDate = issuanceDate,
            maybeExpirationDate = maybeExpirationDate,
            maybeValidFrom = maybeValidFrom,
            maybeValidUntil = maybeValidUntil,
            maybeCredentialSchema = maybeCredentialSchema,
            credentialSubject = credentialSubject,
            maybeCredentialStatus = maybeCredentialStatus,
            maybeRefreshService = maybeRefreshService,
            maybeEvidence = maybeEvidence,
            maybeTermsOfUse = maybeTermsOfUse,
            aud = Set.empty
          )
        }

    implicit val jwtVcDecoder: Decoder[JwtVc] =
      (c: HCursor) =>
        for {
          `@context` <- c
            .downField("@context")
            .as[Set[String]]
            .orElse(c.downField("@context").as[String].map(Set(_)))
          `type` <- c
            .downField("type")
            .as[Set[String]]
            .orElse(c.downField("type").as[String].map(Set(_)))
          maybeCredentialSchema <- c
            .downField("credentialSchema")
            .as[Option[CredentialSchema | List[CredentialSchema]]]
          credentialSubject <- c.downField("credentialSubject").as[Json]
          maybeCredentialStatus <- c.downField("credentialStatus").as[Option[CredentialStatus | List[CredentialStatus]]]
          maybeRefreshService <- c.downField("refreshService").as[Option[RefreshService]]
          maybeEvidence <- c.downField("evidence").as[Option[Json]]
          maybeTermsOfUse <- c.downField("termsOfUse").as[Option[Json]]
          maybeValidFrom <- c.downField("validFrom").as[Option[Instant]]
          maybeValidUntil <- c.downField("validUntil").as[Option[Instant]]
          maybeIssuer <- c.downField("issuer").as[Option[String | CredentialIssuer]]
        } yield {
          JwtVc(
            `@context` = `@context`,
            `type` = `type`,
            maybeCredentialSchema = maybeCredentialSchema,
            credentialSubject = credentialSubject,
            maybeCredentialStatus = maybeCredentialStatus,
            maybeRefreshService = maybeRefreshService,
            maybeEvidence = maybeEvidence,
            maybeTermsOfUse = maybeTermsOfUse,
            maybeValidFrom = maybeValidFrom,
            maybeValidUntil = maybeValidUntil,
            maybeIssuer = maybeIssuer
          )
        }

    implicit val jwtCredentialPayloadDecoder: Decoder[JwtCredentialPayload] =
      (c: HCursor) =>
        for {
          iss <- c.downField("iss").as[String]
          maybeSub <- c.downField("sub").as[Option[String]]
          vc <- c.downField("vc").as[JwtVc]
          nbf <- c.downField("nbf").as[Instant]
          aud <- c
            .downField("aud")
            .as[Option[String]]
            .map(_.iterator.toSet)
            .orElse(c.downField("aud").as[Option[Set[String]]].map(_.iterator.toSet.flatten))
          maybeExp <- c.downField("exp").as[Option[Instant]]
          maybeJti <- c.downField("jti").as[Option[String]]
        } yield {
          JwtCredentialPayload(
            iss = iss,
            maybeSub = maybeSub,
            vc = vc,
            nbf = nbf,
            aud = aud,
            maybeExp = maybeExp,
            maybeJti = maybeJti
          )
        }

    implicit val w3cVerifiableCredentialPayloadDecoder: Decoder[W3cVerifiableCredentialPayload] =
      (c: HCursor) =>
        for {
          payload <- c.as[W3cCredentialPayload]
          proof <- c.downField("proof").as[JwtProof]
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
      jwtVerifiableCredentialPayloadDecoder.or(
        w3cVerifiableCredentialPayloadDecoder.asInstanceOf[Decoder[VerifiableCredentialPayload]]
      )
  }
}

object CredentialVerification {

  import CredentialPayload.Implicits.*

  def validateValidFromNotAfterValidUntil(
      maybeValidFrom: Option[Instant],
      maybeValidUntil: Option[Instant],
      validFromName: String,
      validUntilName: String
  ): Validation[String, Unit] = {
    (maybeValidFrom, maybeValidUntil)
      .mapN((validFrom, validUntil) =>
        if (validFrom.isAfter(validUntil))
          Validation.fail(
            s"Credential cannot expire before being in effect. $validFromName=$validFrom $validUntilName=$validUntil"
          )
        else Validation.unit
      )
      .getOrElse(Validation.unit)
  }

  private def validateValidFrom(
      maybeValidFrom: Option[Instant],
      now: Instant,
      leeway: TemporalAmount,
      validFromName: String,
  ): Validation[String, Unit] = {
    maybeValidFrom
      .map(validFrom =>
        if (now.isBefore(validFrom.minus(leeway)))
          Validation.fail(s"Credential is not yet in effect. now=$now $validFromName=$validFrom leeway=$leeway")
        else Validation.unit
      )
      .getOrElse(Validation.unit)
  }

  private def validateValidUntil(
      maybeValidUntil: Option[Instant],
      now: Instant,
      leeway: TemporalAmount,
      validUntilName: String,
  ): Validation[String, Unit] = {
    maybeValidUntil
      .map(validUntil =>
        if (now.isAfter(validUntil.plus(leeway)))
          Validation.fail(s"Credential has expired. now=$now $validUntilName=$validUntil leeway=$leeway")
        else Validation.unit
      )
      .getOrElse(Validation.unit)
  }

  def verifyDates(
      maybeValidFrom: Option[Instant],
      maybeValidUntil: Option[Instant],
      leeway: TemporalAmount,
      validFromName: String,
      validUntilName: String
  )(implicit
      clock: Clock
  ): Validation[String, Unit] = {
    val now = clock.instant()
    Validation.validateWith(
      validateValidFromNotAfterValidUntil(maybeValidFrom, maybeValidUntil, validFromName, validUntilName),
      validateValidFrom(maybeValidFrom, now, leeway, validFromName),
      validateValidUntil(maybeValidUntil, now, leeway, validUntilName)
    )((l, _, _) => l)
  }

  /** Defines what to verify in the jwt credentials.
    *
    * @param verifySignature
    *   verifies signature using the resolved did.
    * @param verifyDates
    *   verifies issuance and expiration dates.
    * @param leeway
    *   defines the duration we should subtract from issuance date and add to expiration dates.
    * @param maybeProofPurpose
    *   specifies the which type of public key to use in the resolved DidDocument. If empty, we will validate against
    *   all public key.
    */
  case class CredentialVerificationOptions(
      verifySignature: Boolean = true,
      verifyDates: Boolean = false,
      leeway: TemporalAmount = Duration.Zero,
      maybeProofPurpose: Option[VerificationRelationship] = None
  )

  /** Verifies a jwt credential.
    *
    * @param jwt
    *   credential to verify.
    * @param options
    *   defines what to verify.
    * @param didResolver
    *   is used to resolve the did.
    * @param clock
    *   is used to get current time.
    * @return
    *   the result of the validation.
    */
  def verify(verifiableCredentialPayload: VerifiableCredentialPayload, options: CredentialVerificationOptions)(
      didResolver: DidResolver,
      uriResolver: UriResolver
  )(implicit clock: Clock): IO[String, Validation[String, Unit]] = {
    verifiableCredentialPayload match {
      case w3cVerifiableCredentialPayload: W3cVerifiableCredentialPayload =>
        W3CCredential.verify(w3cVerifiableCredentialPayload, options)(didResolver, uriResolver)
      case jwtVerifiableCredentialPayload: JwtVerifiableCredentialPayload =>
        JwtCredential.verify(jwtVerifiableCredentialPayload, options)(didResolver, uriResolver)
    }
  }

  def verifyCredentialStatus(
      credentialStatus: CredentialStatus
  )(uriResolver: UriResolver): IO[String, Validation[String, Unit]] = {

    val res = for {
      statusListString <- uriResolver
        .resolve(credentialStatus.statusListCredential)
        .mapError(err => s"Could not resolve status list credential: $err")
      _ <- ZIO.logInfo("Credential status: " + credentialStatus)
      vcStatusListCredJson <- ZIO
        .fromEither(io.circe.parser.parse(statusListString))
        .mapError(err => s"Could not parse status list credential as Json string: $err")
      statusListCredJsonWithoutProof = vcStatusListCredJson.hcursor.downField("proof").delete.top.get
      proof <- ZIO
        .fromEither(vcStatusListCredJson.hcursor.downField("proof").as[Proof])
        .mapError(err => s"Could not extract proof from status list credential: $err")

      // Verify proof
      verified <- proof match
        case EddsaJcs2022Proof(proofValue, verificationMethod, maybeCreated) =>
          val publicKeyMultiBaseEffect = uriResolver
            .resolve(verificationMethod)
            .mapError(_.toThrowable)
            .flatMap { jsonResponse =>
              ZIO.fromEither(io.circe.parser.decode[MultiKey](jsonResponse)).mapError(_.fillInStackTrace)
            }
            .mapError(_.getMessage)

          for {
            publicKeyMultiBase <- publicKeyMultiBaseEffect
            verified <- EddsaJcs2022ProofGenerator
              .verifyProof(statusListCredJsonWithoutProof, proofValue, publicKeyMultiBase)
              .mapError(_.getMessage)
          } yield verified

        case EcdsaSecp256k1Signature2019Proof(jws, verificationMethod, _, _, _, _) =>
          val jwkEffect = uriResolver
            .resolve(verificationMethod)
            .mapError(_.toThrowable)
            .flatMap { jsonResponse =>
              ZIO
                .fromEither(io.circe.parser.decode[EcdsaSecp256k1VerificationKey2019](jsonResponse))
                .map(_.publicKeyJwk)
                .mapError(_.fillInStackTrace)
            }
            .mapError(_.getMessage)

          for {
            jwk <- jwkEffect
            x <- ZIO.fromOption(jwk.x).orElseFail("Missing x coordinate in public key")
            y <- ZIO.fromOption(jwk.y).orElseFail("Missing y coordinate in public key")
            _ <- jwk.crv.fold(ZIO.fail("Missing crv in public key")) { crv =>
              if crv != "secp256k1" then ZIO.fail(s"Curve must be secp256k1, got $crv")
              else ZIO.unit
            }
            xBytes = Base64Utils.decodeURL(x)
            yBytes = Base64Utils.decodeURL(y)
            ecPublicKey <- ZIO
              .fromTry(KmpSecp256k1KeyOps.publicKeyFromCoordinate(xBytes, yBytes))
              .map(_.toJavaPublicKey)
              .mapError(_.getMessage)
            verified <- EcdsaSecp256k1Signature2019ProofGenerator
              .verifyProof(statusListCredJsonWithoutProof, jws, ecPublicKey)
              .mapError(_.getMessage)
          } yield verified
        // Note: add other proof types here when available

        case _ => ZIO.fail(s"Unsupported proof type - ${proof.`type`}")

      proofVerificationValidation =
        if (verified) Validation.unit else Validation.fail("Could not verify status list credential proof")

      // Check revocation status in the list by index
      encodedBitStringEither = vcStatusListCredJson.hcursor
        .downField("credentialSubject")
        .as[Json]
        .flatMap(_.hcursor.downField("encodedList").as[String])
      encodedBitString <- ZIO.fromEither(encodedBitStringEither).mapError(_.getMessage)
      bitString <- BitString.valueOf(encodedBitString).mapError(_.message)
      isRevoked <- bitString.isRevoked(credentialStatus.statusListIndex).mapError(_.message)
      revocationValidation = if (isRevoked) Validation.fail("Credential is revoked") else Validation.unit

    } yield Validation.validateWith(proofVerificationValidation, revocationValidation)((a, _) => a)

    res
  }
}

object JwtCredential {

  import CredentialPayload.Implicits.*

  def encodeJwt(payload: JwtCredentialPayload, issuer: Issuer): JWT = issuer.signer.encode(payload.asJson)

  def decodeJwt(jwt: JWT, publicKey: PublicKey): Try[JwtCredentialPayload] = {
    val signedJWT = SignedJWT.parse(jwt.value)
    val verifier = JWTVerification.toECDSAVerifier(publicKey)

    val isSignatureValid = signedJWT.verify(verifier)

    if isSignatureValid then
      val claimsSet = signedJWT.getJWTClaimsSet.toString
      decode[JwtCredentialPayload](claimsSet).toTry
    else Failure(Exception(s"Invalid JWT signature for: ${JWT.value}"))
  }

  def decodeJwt(jwt: JWT): IO[String, JwtCredentialPayload] = {
    val decodeJWT =
      ZIO.fromTry(JwtCirce.decodeRawAll(jwt.value, JwtOptions(false, false, false))).mapError(_.getMessage)

    val validatedDecodedClaim: IO[String, JwtCredentialPayload] =
      for {
        decodedJwtTask <- decodeJWT
        (_, claim, _) = decodedJwtTask
        decodedClaim <- ZIO.fromEither(decode[JwtCredentialPayload](claim).left.map(_.toString))
      } yield decodedClaim

    validatedDecodedClaim
  }

  def validateEncodedJwt(jwt: JWT, publicKey: PublicKey): Boolean =
    val signedJWT = SignedJWT.parse(jwt.value)
    val verifier = JWTVerification.toECDSAVerifier(publicKey)
    signedJWT.verify(verifier)

  def validateEncodedJWT(
      jwt: JWT,
      proofPurpose: Option[VerificationRelationship] = None
  )(didResolver: DidResolver): IO[String, Validation[String, Unit]] = {
    JWTVerification.validateEncodedJwt(jwt, proofPurpose)(didResolver: DidResolver)(claim =>
      Validation.fromEither(decode[JwtCredentialPayload](claim).left.map(_.toString))
    )(_.iss)
  }

  def validateIssuerJWT(
      jwt: JWT,
  )(didResolver: DidResolver): IO[String, Validation[String, DIDDocument]] = {
    JWTVerification.validateIssuer(jwt)(didResolver: DidResolver)(claim =>
      Validation.fromEither(decode[JwtCredentialPayload](claim).left.map(_.toString))
    )(_.iss)
  }

  def validateExpiration(jwt: JWT, dateTime: OffsetDateTime): Validation[String, Unit] = {
    Validation
      .fromTry(
        JwtCirce(Clock.fixed(dateTime.toInstant, ZoneId.of(dateTime.getOffset.getId)))
          .decodeRawAll(jwt.value, JwtOptions(false, true, false))
      )
      .flatMap(_ => Validation.unit)
      .mapError(_.getMessage)
  }

  def validateNotBefore(jwt: JWT, dateTime: OffsetDateTime): Validation[String, Unit] = {
    Validation
      .fromTry(
        JwtCirce(Clock.fixed(dateTime.toInstant, ZoneId.of(dateTime.getOffset.getId)))
          .decodeRawAll(jwt.value, JwtOptions(false, false, true))
      )
      .flatMap(_ => Validation.unit)
      .mapError(_.getMessage)
  }

  def verifyDates(jwt: JWT, leeway: TemporalAmount)(implicit clock: Clock): Validation[String, Unit] = {
    val decodeJWT =
      Validation
        .fromTry(JwtCirce.decodeRaw(jwt.value, options = JwtOptions(false, false, false)))
        .mapError(_.getMessage)

    for {
      decodedJWT <- decodeJWT
      jwtCredentialPayload <- Validation.fromEither(decode[JwtCredentialPayload](decodedJWT)).mapError(_.getMessage)
      nbf = jwtCredentialPayload.nbf
      maybeExp = jwtCredentialPayload.maybeExp
      maybeValidFrom = jwtCredentialPayload.vc.maybeValidFrom
      maybeValidUntil = jwtCredentialPayload.vc.maybeValidUntil
      result <- Validation.validateWith(
        CredentialVerification.verifyDates(maybeValidFrom, maybeValidUntil, leeway, "validFrom", "validUntil")(clock),
        CredentialVerification.verifyDates(Some(nbf), maybeExp, leeway, "nbf", "exp")(clock)
      )((l, _) => l)
    } yield result
  }

  def verify(jwt: JwtVerifiableCredentialPayload, options: CredentialVerification.CredentialVerificationOptions)(
      didResolver: DidResolver,
      uriResolver: UriResolver
  )(implicit clock: Clock): IO[String, Validation[String, Unit]] =
    verify(jwt.jwt, options)(didResolver, uriResolver)(clock)

  def verify(jwt: JWT, options: CredentialVerification.CredentialVerificationOptions)(
      didResolver: DidResolver,
      uriResolver: UriResolver
  )(implicit clock: Clock): IO[String, Validation[String, Unit]] = {
    for {
      signatureValidation <-
        if (options.verifySignature) then validateEncodedJWT(jwt, options.maybeProofPurpose)(didResolver)
        else ZIO.succeed(Validation.unit)
      dateVerification <- ZIO.succeed(
        if (options.verifyDates) then verifyDates(jwt, options.leeway) else Validation.unit
      )
      revocationVerification <- verifyRevocationStatusJwt(jwt)(uriResolver)

    } yield Validation.validateWith(signatureValidation, dateVerification, revocationVerification)((a, _, _) => a)
  }

  def verifyRevocationStatusJwt(jwt: JWT)(uriResolver: UriResolver): IO[String, Validation[String, Unit]] = {
    val decodeJWT =
      ZIO
        .fromTry(JwtCirce.decodeRaw(jwt.value, options = JwtOptions(false, false, false)))
        .mapError(_.getMessage)

    val res = for {
      decodedJWT <- decodeJWT
      jwtCredentialPayload <- ZIO.fromEither(decode[JwtCredentialPayload](decodedJWT)).mapError(_.getMessage)
      credentialStatus = jwtCredentialPayload.vc.maybeCredentialStatus
        .map {
          {
            case status: CredentialStatus           => List(status)
            case statusList: List[CredentialStatus] => statusList
          }
        }
        .getOrElse(List.empty)
      results <- ZIO.collectAll(
        credentialStatus.map(status => CredentialVerification.verifyCredentialStatus(status)(uriResolver))
      )
      result = Validation.validateAll(results).flatMap(_ => Validation.unit)
    } yield result
    res
  }
}

object W3CCredential {

  import CredentialPayload.Implicits.*

  def encodeW3C(payload: W3cCredentialPayload, issuer: Issuer): W3cVerifiableCredentialPayload = {
    W3cVerifiableCredentialPayload(
      payload = payload,
      proof = JwtProof(
        `type` = "JwtProof2020",
        jwt = issuer.signer.encode(payload.asJson)
      )
    )
  }

  def toEncodedJwt(payload: W3cCredentialPayload, issuer: Issuer): JWT =
    JwtCredential.encodeJwt(payload.toJwtCredentialPayload, issuer)

  def toJsonWithEmbeddedProof(payload: W3cCredentialPayload, issuer: Issuer): Task[Json] = {
    val jsonCred = payload.asJson

    for {
      proof <- issuer.signer.generateProofForJson(jsonCred, issuer.publicKey)
      jsonProof <- proof match
        case b: EcdsaSecp256k1Signature2019Proof => ZIO.succeed(b.asJson.dropNullValues)
        case c: EddsaJcs2022Proof                => ZIO.succeed(c.asJson.dropNullValues)
        case _: DataIntegrityProof               => UnexpectedCodeExecutionPath
      verifiableCredentialWithProof = jsonCred.deepMerge(Map("proof" -> jsonProof).asJson)
    } yield verifiableCredentialWithProof

  }

  def validateW3C(
      payload: W3cVerifiableCredentialPayload,
      proofPurpose: Option[VerificationRelationship] = None
  )(didResolver: DidResolver): IO[String, Validation[String, Unit]] = {
    JWTVerification.validateEncodedJwt(payload.proof.jwt, proofPurpose)(didResolver: DidResolver)(claim =>
      Validation.fromEither(decode[W3cCredentialPayload](claim).left.map(_.toString))
    )(vc =>
      vc.issuer match {
        case string: String                     => string
        case credentialIssuer: CredentialIssuer => credentialIssuer.id
      }
    )
  }

  def verifyDates(w3cPayload: W3cVerifiableCredentialPayload, leeway: TemporalAmount)(implicit
      clock: Clock
  ): Validation[String, Unit] = {
    Validation.validateWith(
      CredentialVerification.verifyDates(
        w3cPayload.payload.maybeValidFrom,
        w3cPayload.payload.maybeValidUntil,
        leeway,
        "validFrom",
        "validUntil"
      )(clock),
      CredentialVerification.verifyDates(
        Some(w3cPayload.payload.issuanceDate),
        w3cPayload.payload.maybeExpirationDate,
        leeway,
        "issuanceDate",
        "expirationDate"
      )(
        clock
      )
    )((l, _) => l)
  }

  private def verifyRevocationStatusW3c(
      w3cPayload: W3cVerifiableCredentialPayload,
  )(uriResolver: UriResolver): IO[String, Validation[String, Unit]] = {
    val credentialStatus = w3cPayload.payload.maybeCredentialStatus
      .map {
        {
          case status: CredentialStatus           => List(status)
          case statusList: List[CredentialStatus] => statusList
        }
      }
      .getOrElse(List.empty)
    for {
      results <- ZIO.collectAll(
        credentialStatus.map(status => CredentialVerification.verifyCredentialStatus(status)(uriResolver))
      )
      result = Validation.validateAll(results).flatMap(_ => Validation.unit)
    } yield result
  }

  def verify(w3cPayload: W3cVerifiableCredentialPayload, options: CredentialVerification.CredentialVerificationOptions)(
      didResolver: DidResolver,
      uriResolver: UriResolver
  )(implicit clock: Clock): IO[String, Validation[String, Unit]] = {
    for {
      signatureValidation <-
        if (options.verifySignature) then validateW3C(w3cPayload, options.maybeProofPurpose)(didResolver)
        else ZIO.succeed(Validation.unit)
      dateVerification <- ZIO.succeed(
        if (options.verifyDates) then verifyDates(w3cPayload, options.leeway) else Validation.unit
      )
      revocationVerification <- verifyRevocationStatusW3c(w3cPayload)(uriResolver)
    } yield Validation.validateWith(signatureValidation, dateVerification, revocationVerification)((a, _, _) => a)
  }
}
