package io.iohk.atala.pollux.vc.jwt

import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*
import com.nimbusds.jose.util.Base64URL
import io.circe
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.pollux.vc.jwt.schema.{SchemaResolver, SchemaValidator}
import net.reactivecore.cjs.validator.Violation
import net.reactivecore.cjs.{DocumentValidator, Loader}
import pdi.jwt.*
import zio.prelude.*
import zio.{Duration, IO, NonEmptyChunk, Task, ZIO}

import java.security.spec.{ECParameterSpec, ECPublicKeySpec}
import java.security.{KeyPairGenerator, PublicKey}
import java.time.temporal.{Temporal, TemporalAmount, TemporalUnit}
import java.time.{Clock, Instant, ZonedDateTime}
import java.util
import scala.util.{Failure, Success, Try}
opaque type DID = String

object DID {
  def apply(value: String): DID = value

  extension (did: DID) {
    def value: String = did
  }
}

case class Issuer(did: DID, signer: Signer, publicKey: PublicKey)

sealed trait VerifiableCredentialPayload

case class W3cVerifiableCredentialPayload(payload: W3cCredentialPayload, proof: Proof)
    extends Verifiable(proof),
      VerifiableCredentialPayload

case class JwtVerifiableCredentialPayload(jwt: JWT) extends VerifiableCredentialPayload

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

sealed trait CredentialPayload {
  def maybeSub: Option[String]

  def `@context`: Set[String]

  def `type`: Set[String]

  def maybeJti: Option[String]

  def nbf: Instant

  def aud: Set[String]

  def maybeExp: Option[Instant]

  def iss: String

  def maybeCredentialStatus: Option[CredentialStatus]

  def maybeRefreshService: Option[RefreshService]

  def maybeEvidence: Option[Json]

  def maybeTermsOfUse: Option[Json]

  def maybeCredentialSchema: Option[CredentialSchema]

  def credentialSubject: Json

  def toJwtCredentialPayload: JwtCredentialPayload =
    JwtCredentialPayload(
      iss = iss,
      maybeSub = maybeSub,
      vc = JwtVc(
        `@context` = `@context`,
        `type` = `type`,
        maybeCredentialSchema = maybeCredentialSchema,
        credentialSubject = credentialSubject,
        maybeCredentialStatus = maybeCredentialStatus,
        maybeRefreshService = maybeRefreshService,
        maybeEvidence = maybeEvidence,
        maybeTermsOfUse = maybeTermsOfUse
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
      issuer = DID(iss),
      issuanceDate = nbf,
      maybeExpirationDate = maybeExp,
      maybeCredentialSchema = maybeCredentialSchema,
      credentialSubject = credentialSubject,
      maybeCredentialStatus = maybeCredentialStatus,
      maybeRefreshService = maybeRefreshService,
      maybeEvidence = maybeEvidence,
      maybeTermsOfUse = maybeTermsOfUse,
      aud = aud
    )
}

object CredentialPayloadValidation {

  val DEFAULT_VC_TYPE = "VerifiableCredential"
  val DEFAULT_CONTEXT = "https://www.w3.org/2018/credentials/v1"

  def validateIssuerDid(maybeIssuerDid: Option[String]): Validation[String, String] = {
    Validation
      .fromOptionWith("Issuer Did is empty")(maybeIssuerDid)
  }

  def validateIssuanceDate(maybeIssuanceDate: Option[Instant]): Validation[String, Instant] = {
    Validation
      .fromOptionWith("Issuance Date is empty")(maybeIssuanceDate)
  }

  def validateContext(`@context`: Set[String]): Validation[String, NonEmptySet[String]] = {
    Validation
      .fromOptionWith("@context is empty")(`@context`.toNonEmptySet)
      .flatMap(`nonEmpty@context` =>
        Validation.fromPredicateWith("Missing default context from @context")(`nonEmpty@context`)(
          _.contains(DEFAULT_CONTEXT)
        )
      )
  }

  def validateVcType(`type`: Set[String]): Validation[String, NonEmptySet[String]] = {
    Validation
      .fromOptionWith("Type is empty")(`type`.toNonEmptySet)
      .flatMap(nonEmptyType =>
        Validation.fromPredicateWith("Missing default type from `type`")(nonEmptyType)(
          _.contains(DEFAULT_VC_TYPE)
        )
      )
  }

  def validateCredentialSchema(
      maybeCredentialSchema: Option[Json]
  )(schemaToValidator: Json => Validation[String, SchemaValidator]): Validation[String, Option[SchemaValidator]] = {
    maybeCredentialSchema.fold(Validation.succeed(Option.empty))(credentialSchema => {
      schemaToValidator(credentialSchema).map(Some(_))
    })
  }

  def validateCredentialSubjectSchema(
      credentialSubject: Json,
      credentialSchemaValidator: SchemaValidator
  ): Validation[String, Json] =
    credentialSchemaValidator.validate(credentialSubject)

  def validateCredentialSubject(
      credentialSubject: Json,
      maybeCredentialSchemaValidator: Option[SchemaValidator]
  ): Validation[String, Json] = {
    for {
      validatedCredentialSubjectNotEmpty <- validateCredentialSubjectNotEmpty(credentialSubject)
      validatedCredentialSubjectHasId <- validateCredentialSubjectHasId(validatedCredentialSubjectNotEmpty)
      validatedCredentialSubjectSchema <- maybeCredentialSchemaValidator
        .map(validateCredentialSubjectSchema(validatedCredentialSubjectHasId, _))
        .getOrElse(Validation.succeed(validatedCredentialSubjectHasId))
    } yield validatedCredentialSubjectSchema
  }

  def validate[C <: CredentialPayload](credentialPayload: C): Validation[String, C] =
    Validation.validateWith(
      CredentialPayloadValidation.validateContext(credentialPayload.`@context`),
      CredentialPayloadValidation.validateVcType(credentialPayload.`type`)
    ) { (`@context`, `type`) => credentialPayload }

  def validateSchema[C <: CredentialPayload](credentialPayload: C)(schemaResolver: SchemaResolver)(
      schemaToValidator: Json => Validation[String, SchemaValidator]
  ): IO[String, C] =
    val validation =
      for {
        resolvedSchema <- ZIO.foreach(credentialPayload.maybeCredentialSchema)(schemaResolver.resolve)
        maybeDocumentValidator <- CredentialPayloadValidation
          .validateCredentialSchema(resolvedSchema)(schemaToValidator)
          .toZIO
        maybeValidatedCredentialSubject <- CredentialPayloadValidation
          .validateCredentialSubject(
            credentialPayload.credentialSubject,
            maybeDocumentValidator
          )
          .toZIO
      } yield maybeValidatedCredentialSubject
    validation.map(_ => credentialPayload)

  private def validateCredentialSubjectNotEmpty(credentialSubject: Json): Validation[String, Json] = {
    Validation
      .fromPredicateWith("credentialSubject is empty.")(credentialSubject)(_.isObject)
  }

  private def validateCredentialSubjectHasId(credentialSubject: Json): Validation[String, Json] = {
    Validation
      .fromPredicateWith("credentialSubject must contain id.")(credentialSubject)(
        _.asObject.exists(jsonObject => jsonObject.toMap.contains("id"))
      )
  }
}

case class JwtVc(
    `@context`: Set[String],
    `type`: Set[String],
    maybeCredentialSchema: Option[CredentialSchema],
    credentialSubject: Json,
    maybeCredentialStatus: Option[CredentialStatus],
    maybeRefreshService: Option[RefreshService],
    maybeEvidence: Option[Json],
    maybeTermsOfUse: Option[Json]
)

case class JwtCredentialPayload(
    override val iss: String,
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
}

case class W3cCredentialPayload(
    override val `@context`: Set[String],
    override val `type`: Set[String],
    val maybeId: Option[String],
    val issuer: DID,
    val issuanceDate: Instant,
    val maybeExpirationDate: Option[Instant],
    override val maybeCredentialSchema: Option[CredentialSchema],
    override val credentialSubject: Json,
    override val maybeCredentialStatus: Option[CredentialStatus],
    override val maybeRefreshService: Option[RefreshService],
    override val maybeEvidence: Option[Json],
    override val maybeTermsOfUse: Option[Json],
    override val aud: Set[String] = Set.empty
) extends CredentialPayload {
  override val maybeSub = credentialSubject.hcursor.downField("id").as[String].toOption
  override val maybeJti = maybeId
  override val nbf = issuanceDate
  override val maybeExp = maybeExpirationDate
  override val iss = issuer.value
}

object CredentialPayload {
  object Implicits {

    import InstantDecoderEncoder.*
    import Proof.Implicits.*

    implicit val didEncoder: Encoder[DID] =
      (did: DID) => did.value.asJson

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
            ("type", w3cCredentialPayload.`type`.asJson),
            ("id", w3cCredentialPayload.maybeId.asJson),
            ("issuer", w3cCredentialPayload.issuer.asJson),
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

    implicit val credentialStatusDecoder: Decoder[CredentialStatus] =
      (c: HCursor) =>
        for {
          id <- c.downField("id").as[String]
          `type` <- c.downField("type").as[String]
        } yield {
          CredentialStatus(id = id, `type` = `type`)
        }

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
            `@context` = `@context`,
            `type` = `type`,
            maybeId = maybeId,
            issuer = DID(issuer),
            issuanceDate = issuanceDate,
            maybeExpirationDate = maybeExpirationDate,
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
          maybeCredentialSchema <- c.downField("credentialSchema").as[Option[CredentialSchema]]
          credentialSubject <- c.downField("credentialSubject").as[Json]
          maybeCredentialStatus <- c.downField("credentialStatus").as[Option[CredentialStatus]]
          maybeRefreshService <- c.downField("refreshService").as[Option[RefreshService]]
          maybeEvidence <- c.downField("evidence").as[Option[Json]]
          maybeTermsOfUse <- c.downField("termsOfUse").as[Option[Json]]
        } yield {
          JwtVc(
            `@context` = `@context`,
            `type` = `type`,
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
      jwtVerifiableCredentialPayloadDecoder.or(
        w3cVerifiableCredentialPayloadDecoder.asInstanceOf[Decoder[VerifiableCredentialPayload]]
      )
  }
}

object CredentialVerification {

  import CredentialPayload.Implicits.*

  def validateCredential(
      verifiableCredentialPayload: VerifiableCredentialPayload
  )(didResolver: DidResolver): IO[String, Validation[String, Unit]] = {
    verifiableCredentialPayload match {
      case (w3cVerifiableCredentialPayload: W3cVerifiableCredentialPayload) =>
        W3CCredential.validateW3C(w3cVerifiableCredentialPayload)(didResolver)
      case (jwtVerifiableCredentialPayload: JwtVerifiableCredentialPayload) =>
        JwtCredential.validateEncodedJWT(jwtVerifiableCredentialPayload.jwt)(didResolver)
    }
  }

  def verifyDates(issuanceDate: Instant, maybeExpirationDate: Option[Instant], leeway: TemporalAmount)(implicit
      clock: Clock
  ): Validation[String, Unit] = {
    val now = clock.instant()

    def validateNbfNotAfterExp(nbf: Instant, maybeExp: Option[Instant]): Validation[String, Unit] = {
      maybeExp
        .map(exp =>
          if (nbf.isAfter(exp))
            Validation.fail(s"Credential cannot expire before being in effect. nbf=$nbf exp=$exp")
          else Validation.unit
        )
        .getOrElse(Validation.unit)
    }

    def validateNbf(nbf: Instant): Validation[String, Unit] = {
      if (now.isBefore(nbf.minus(leeway)))
        Validation.fail(s"Credential is not yet in effect. now=$now nbf=$nbf leeway=$leeway")
      else Validation.unit
    }

    def validateExp(maybeExp: Option[Instant]): Validation[String, Unit] = {
      maybeExp
        .map(exp =>
          if (now.isAfter(exp.plus(leeway)))
            Validation.fail(s"Credential has expired. now=$now exp=$exp leeway=$leeway")
          else Validation.unit
        )
        .getOrElse(Validation.unit)
    }

    Validation.validateWith(
      validateNbfNotAfterExp(issuanceDate, maybeExpirationDate),
      validateNbf(issuanceDate),
      validateExp(maybeExpirationDate)
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
      didResolver: DidResolver
  )(implicit clock: Clock): IO[String, Validation[String, Unit]] = {
    verifiableCredentialPayload match {
      case (w3cVerifiableCredentialPayload: W3cVerifiableCredentialPayload) =>
        W3CCredential.verify(w3cVerifiableCredentialPayload, options)(didResolver)
      case (jwtVerifiableCredentialPayload: JwtVerifiableCredentialPayload) =>
        JwtCredential.verify(jwtVerifiableCredentialPayload, options)(didResolver)
    }
  }
}

object JwtCredential {

  import CredentialPayload.Implicits.*

  def encodeJwt(payload: JwtCredentialPayload, issuer: Issuer): JWT = issuer.signer.encode(payload.asJson)

  def decodeJwt(jwt: JWT, publicKey: PublicKey): Try[JwtCredentialPayload] = {
    JwtCirce
      .decodeRaw(jwt.value, publicKey, options = JwtOptions(expiration = false, notBefore = false))
      .flatMap(decode[JwtCredentialPayload](_).toTry)
  }

  def validateEncodedJwt(jwt: JWT, publicKey: PublicKey): Boolean =
    JwtCirce.isValid(jwt.value, publicKey, JwtOptions(expiration = false, notBefore = false))

  def validateEncodedJWT(
      jwt: JWT,
      proofPurpose: Option[VerificationRelationship] = None
  )(didResolver: DidResolver): IO[String, Validation[String, Unit]] = {
    JWTVerification.validateEncodedJwt(jwt, proofPurpose)(didResolver: DidResolver)(claim =>
      Validation.fromEither(decode[JwtCredentialPayload](claim).left.map(_.toString))
    )(_.iss)
  }

  def validateJwtSchema(
      jwt: JWT
  )(schemaResolver: SchemaResolver)(
      schemaToValidator: Json => Validation[String, SchemaValidator]
  ): IO[String, Validation[String, Unit]] = {
    val decodeJWT =
      Validation.fromTry(JwtCirce.decodeRawAll(jwt.value, JwtOptions(false, false, false))).mapError(_.getMessage)

    val validatedDecodedClaim: Validation[String, JwtCredentialPayload] =
      for {
        decodedJwtTask <- decodeJWT
        (_, claim, _) = decodedJwtTask
        decodedClaim <- Validation.fromEither(decode[JwtCredentialPayload](claim).left.map(_.toString))
      } yield decodedClaim

    ValidationUtils.foreach(
      validatedDecodedClaim.map(decodedClaim =>
        CredentialPayloadValidation.validateSchema(decodedClaim)(schemaResolver)(schemaToValidator)
      )
    )(_.replicateZIODiscard(1))
  }

  def validateSchemaAndSignature(
      jwt: JWT
  )(didResolver: DidResolver)(schemaResolver: SchemaResolver)(
      schemaToValidator: Json => Validation[String, SchemaValidator]
  ): IO[String, Validation[String, Unit]] = {
    for {
      validatedJwtSchema <- validateJwtSchema(jwt)(schemaResolver)(schemaToValidator)
      validateJwtSignature <- validateEncodedJWT(jwt)(didResolver)
    } yield {
      Validation.validateWith(validatedJwtSchema, validateJwtSignature)((a, _) => a)
    }
  }

  def verifyDates(jwtPayload: JwtVerifiableCredentialPayload, leeway: TemporalAmount)(implicit
      clock: Clock
  ): Validation[String, Unit] = {
    verifyDates(jwtPayload.jwt, leeway)(clock)
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
      result <- CredentialVerification.verifyDates(nbf, maybeExp, leeway)(clock)
    } yield result
  }

  def verify(jwt: JwtVerifiableCredentialPayload, options: CredentialVerification.CredentialVerificationOptions)(
      didResolver: DidResolver
  )(implicit clock: Clock): IO[String, Validation[String, Unit]] = verify(jwt.jwt, options)(didResolver)(clock)

  def verify(jwt: JWT, options: CredentialVerification.CredentialVerificationOptions)(
      didResolver: DidResolver
  )(implicit clock: Clock): IO[String, Validation[String, Unit]] = {
    for {
      signatureValidation <-
        if (options.verifySignature) then validateEncodedJWT(jwt, options.maybeProofPurpose)(didResolver)
        else ZIO.succeed(Validation.unit)
      dateVerification <- ZIO.succeed(
        if (options.verifyDates) then verifyDates(jwt, options.leeway) else Validation.unit
      )
    } yield Validation.validateWith(signatureValidation, dateVerification)((a, _) => a)
  }
}

object W3CCredential {

  import CredentialPayload.Implicits.*

  def encodeW3C(payload: W3cCredentialPayload, issuer: Issuer): W3cVerifiableCredentialPayload = {
    W3cVerifiableCredentialPayload(
      payload = payload,
      proof = Proof(
        `type` = "JwtProof2020",
        jwt = issuer.signer.encode(payload.asJson)
      )
    )
  }

  def toEncodedJwt(payload: W3cCredentialPayload, issuer: Issuer): JWT =
    JwtCredential.encodeJwt(payload.toJwtCredentialPayload, issuer)

  def validateW3C(
      payload: W3cVerifiableCredentialPayload,
      proofPurpose: Option[VerificationRelationship] = None
  )(didResolver: DidResolver): IO[String, Validation[String, Unit]] = {
    JWTVerification.validateEncodedJwt(payload.proof.jwt, proofPurpose)(didResolver: DidResolver)(claim =>
      Validation.fromEither(decode[W3cCredentialPayload](claim).left.map(_.toString))
    )(_.issuer.value)
  }

  def verifyDates(w3cPayload: W3cVerifiableCredentialPayload, leeway: TemporalAmount)(implicit
      clock: Clock
  ): Validation[String, Unit] = {
    CredentialVerification.verifyDates(w3cPayload.payload.issuanceDate, w3cPayload.payload.maybeExpirationDate, leeway)(
      clock
    )
  }

  def verify(w3cPayload: W3cVerifiableCredentialPayload, options: CredentialVerification.CredentialVerificationOptions)(
      didResolver: DidResolver
  )(implicit clock: Clock): IO[String, Validation[String, Unit]] = {
    for {
      signatureValidation <-
        if (options.verifySignature) then validateW3C(w3cPayload, options.maybeProofPurpose)(didResolver)
        else ZIO.succeed(Validation.unit)
      dateVerification <- ZIO.succeed(
        if (options.verifyDates) then verifyDates(w3cPayload, options.leeway) else Validation.unit
      )
    } yield Validation.validateWith(signatureValidation, dateVerification)((a, _) => a)
  }
}
