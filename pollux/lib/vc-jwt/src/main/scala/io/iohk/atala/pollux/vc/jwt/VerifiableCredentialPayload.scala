package io.iohk.atala.pollux.vc.jwt

import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*
import com.nimbusds.jose.util.Base64URL
import io.circe
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.iohk.atala.pollux.vc.jwt.schema.SchemaValidator
import net.reactivecore.cjs.validator.Violation
import net.reactivecore.cjs.{DocumentValidator, Loader}
import pdi.jwt.*
import zio.prelude.*
import zio.{IO, NonEmptyChunk, Task, ZIO}

import java.security.spec.{ECParameterSpec, ECPublicKeySpec}
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

sealed trait CredentialPayload(
    maybeSub: Option[String],
    `@context`: Set[String],
    `type`: Set[String],
    maybeJti: Option[String],
    maybeNbf: Option[Instant],
    aud: Set[String],
    maybeExp: Option[Instant],
    maybeIss: Option[String],
    maybeCredentialStatus: Option[CredentialStatus],
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
        maybeCredentialStatus = maybeCredentialStatus,
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
      CredentialPayloadValidation.validateIssuerDid(this.maybeIss),
      CredentialPayloadValidation.validateIssuanceDate(this.maybeNbf)
    ) { (iss, nbf) =>
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

  def toValidatedCredentialPayload(credentialSchemaResolver: CredentialSchema => Json)(
      schemaToValidator: Json => Either[String, SchemaValidator]
  ): Validation[String, ValidatedCredentialPayload] =
    Validation.validateWith(
      CredentialPayloadValidation.validateIssuanceDate(this.maybeNbf),
      CredentialPayloadValidation.validateIssuerDid(this.maybeIss),
      CredentialPayloadValidation.validateContext(this.`@context`),
      CredentialPayloadValidation.validateVcType(this.`type`),
      for {
        maybeDocumentValidator <- CredentialPayloadValidation.validateCredentialSchema(
          this.maybeCredentialSchema.map(credentialSchemaResolver)
        )(schemaToValidator)
        validatedCredentialSubject <- CredentialPayloadValidation
          .validateCredentialSubject(this.credentialSubject, maybeDocumentValidator)
      } yield validatedCredentialSubject
    ) { (nbf, iss, `@context`, `type`, credentialSubject) =>
      ValidatedCredentialPayload(
        maybeSub = maybeSub,
        `@context` = `@context`,
        `type` = `type`,
        maybeJti = maybeJti,
        nbf = nbf,
        aud = aud,
        maybeExp = maybeExp,
        iss = iss,
        maybeCredentialStatus = maybeCredentialStatus,
        maybeRefreshService = maybeRefreshService,
        maybeEvidence = maybeEvidence,
        maybeTermsOfUse = maybeTermsOfUse,
        maybeCredentialSchema = maybeCredentialSchema,
        credentialSubject = credentialSubject
      )
    }
}

case class ValidatedCredentialPayload(
    maybeSub: Option[String],
    `@context`: NonEmptySet[String],
    `type`: NonEmptySet[String],
    maybeJti: Option[String],
    nbf: Instant,
    aud: Set[String],
    maybeExp: Option[Instant],
    iss: String,
    maybeCredentialStatus: Option[CredentialStatus],
    maybeRefreshService: Option[RefreshService],
    maybeEvidence: Option[Json],
    maybeTermsOfUse: Option[Json],
    maybeCredentialSchema: Option[CredentialSchema],
    credentialSubject: Json
) extends CredentialPayload(
      maybeSub = maybeSub,
      `@context` = `@context`.toSet,
      `type` = `type`.toSet,
      maybeJti = maybeJti,
      maybeNbf = Some(nbf),
      aud = aud,
      maybeExp = maybeExp,
      maybeIss = Some(iss),
      maybeCredentialStatus = maybeCredentialStatus,
      maybeRefreshService = maybeRefreshService,
      maybeEvidence = maybeEvidence,
      maybeTermsOfUse = maybeTermsOfUse,
      maybeCredentialSchema = maybeCredentialSchema,
      credentialSubject = credentialSubject
    )

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
  )(schemaToValidator: Json => Either[String, SchemaValidator]): Validation[String, Option[SchemaValidator]] = {
    maybeCredentialSchema.fold(Validation.succeed(Option.empty))(credentialSchema => {
      Validation.fromEither(schemaToValidator(credentialSchema)).map(Some(_))
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
    maybeIss: Option[String],
    maybeSub: Option[String],
    vc: JwtVc,
    maybeNbf: Option[Instant],
    aud: Set[String],
    maybeExp: Option[Instant],
    maybeJti: Option[String]
) extends CredentialPayload(
      maybeSub = maybeSub.orElse(vc.credentialSubject.hcursor.downField("id").as[String].toOption),
      `@context` = vc.`@context`,
      `type` = vc.`type`,
      maybeJti = maybeJti,
      maybeNbf = maybeNbf,
      aud = aud,
      maybeExp = maybeExp,
      maybeIss = maybeIss,
      maybeCredentialStatus = vc.maybeCredentialStatus,
      maybeRefreshService = vc.maybeRefreshService,
      maybeEvidence = vc.maybeEvidence,
      maybeTermsOfUse = vc.maybeTermsOfUse,
      maybeCredentialSchema = vc.maybeCredentialSchema,
      credentialSubject = vc.credentialSubject
    )

case class W3cCredentialPayload(
    `@context`: Set[String],
    `type`: Set[String],
    maybeId: Option[String],
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
    aud: Set[String] = Set.empty
) extends CredentialPayload(
      maybeSub = credentialSubject.hcursor.downField("id").as[String].toOption,
      `@context` = `@context`,
      `type` = `type`,
      maybeJti = maybeId,
      maybeNbf = Some(issuanceDate),
      aud = aud,
      maybeExp = maybeExpirationDate,
      maybeIss = Some(issuer.value),
      maybeCredentialStatus = maybeCredentialStatus,
      maybeRefreshService = maybeRefreshService,
      maybeEvidence = maybeEvidence,
      maybeTermsOfUse = maybeTermsOfUse,
      maybeCredentialSchema = maybeCredentialSchema,
      credentialSubject = credentialSubject
    )

object CredentialPayload {
  object Implicits {

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
          maybeIss <- c.downField("iss").as[Option[String]]
          maybeSub <- c.downField("sub").as[Option[String]]
          vc <- c.downField("vc").as[JwtVc]
          maybeNbf <- c.downField("nbf").as[Option[Instant]]
          aud <- c
            .downField("aud")
            .as[Option[String]]
            .map(_.iterator.toSet)
            .orElse(c.downField("aud").as[Option[Set[String]]].map(_.iterator.toSet.flatten))
          maybeExp <- c.downField("exp").as[Option[Instant]]
          maybeJti <- c.downField("jti").as[Option[String]]
        } yield {
          JwtCredentialPayload(
            maybeIss = maybeIss,
            maybeSub = maybeSub,
            vc = vc,
            maybeNbf = maybeNbf,
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

object JwtCredential {
  import CredentialPayload.Implicits.*

  def encodeJwt(payload: JwtCredentialPayload, issuer: Issuer): JWT = issuer.signer.encode(payload.asJson)

  def toEncodedJwt(payload: W3cCredentialPayload, issuer: Issuer): JWT =
    encodeJwt(payload.toJwtCredentialPayload, issuer)

  def decodeJwt(jwt: JWT, publicKey: PublicKey): Try[JwtCredentialPayload] = {
    JwtCirce.decodeRaw(jwt.value, publicKey).flatMap(decode[JwtCredentialPayload](_).toTry)
  }

  def validateEncodedJwt(jwt: JWT, publicKey: PublicKey): Boolean =
    JwtCirce.isValid(jwt.value, publicKey)

  def validateEncodedJWT(jwt: JWT, verificationMethods: Vector[VerificationMethod]): Boolean = {
    verificationMethods.exists(verificationMethod =>
      toPublicKey(verificationMethod).exists(publicKey => validateEncodedJwt(jwt, publicKey))
    )
  }

  // Review Self Issuance
  def validateEncodedJWT(jwt: JWT)(didResolver: DidResolver)(credentialSchemaResolver: CredentialSchema => Json)(
      schemaToValidator: Json => Either[String, SchemaValidator]
  ): IO[Throwable, Validation[String, IO[Throwable, Validation[String, Boolean]]]] = {
    val decodedJwtIO: IO[Throwable, (String, String, String)] =
      ZIO.fromTry(JwtCirce.decodeRawAll(jwt.value, JwtOptions(false, false, false)))

    val validatedJwtAlgorithmIO: IO[Throwable, Validation[String, JwtAlgorithm]] =
      decodedJwtIO.map((header, _, _) =>
        Validation.fromOptionWith("An algorithm must be specified in the header")(
          JwtCirce.parseHeader(header).algorithm
        )
      )

    val validatedIssuerDidIO: IO[Throwable, Validation[String, String]] =
      decodedJwtIO
        .map((_, claim, _) => claim)
        .map(claim => Validation.fromTry(decode[JwtCredentialPayload](claim).toTry).mapError(_.getMessage))
        .map(_.flatMap(b => b.toValidatedCredentialPayload(credentialSchemaResolver)(schemaToValidator).map(_.iss)))

    def resolveDidDocument(
        issuerDid: _root_.java.lang.String
    ): IO[Throwable, Validation[String, DIDDocument]] = {
      for {
        didResolutionResult <- didResolver.resolve(issuerDid)
      } yield didResolutionResult match
        case (didResolutionSucceeded: DIDResolutionSucceeded) =>
          Validation.succeed(didResolutionSucceeded.didDocument)
        case (didResolutionFailed: DIDResolutionFailed) => Validation.fail(didResolutionFailed.error.toString)
    }

    def validatedVerificationMethods(didDocument: DIDDocument, jwtAlgorithm: JwtAlgorithm) = {
      Validation.fromPredicateWith("No PublicKey to validate against found")(
        didDocument.verificationMethod.filter(verification => verification.`type` == jwtAlgorithm.name)
      )(_.nonEmpty)
    }

    for {
      validatedIssuerDid <- validatedIssuerDidIO
    } yield for {
      issuerDid <- validatedIssuerDid
    } yield for {
      validatedJwtAlgorithm <- validatedJwtAlgorithmIO
      validatedDidDocument <- resolveDidDocument(issuerDid)
    } yield for {
      jwtAlgorithm <- validatedJwtAlgorithm
      didDocument <- validatedDidDocument
      verificationMethods <- validatedVerificationMethods(didDocument, jwtAlgorithm)
    } yield validateEncodedJWT(jwt, verificationMethods)
  }

  // TODO Review DID self-issuance
  def verify(
      payload: ValidatedCredentialPayload,
      jwt: JWT,
      verificationAlgorithm: JwtAlgorithm,
      didResolver: DidResolver
  ): IO[Throwable, Validation[String, Boolean]] = {

    val did = payload.iss
    val validatedDidDocumentIO =
      for {
        didResolutionResult <- didResolver.resolve(did)
      } yield didResolutionResult match
        case (didResolutionSucceeded: DIDResolutionSucceeded) => Validation.succeed(didResolutionSucceeded.didDocument)
        case (didResolutionFailed: DIDResolutionFailed)       => Validation.fail(didResolutionFailed.error.toString)

    val validatedVerificationMethodsIO =
      for {
        validatedDidDocument <- validatedDidDocumentIO
      } yield for {
        didDocument <- validatedDidDocument
        verificationMethods <- Validation.fromPredicateWith("No PublicKey to validate against found")(
          didDocument.verificationMethod.filter(verification => verification.`type` == verificationAlgorithm.name)
        )(_.nonEmpty)
      } yield verificationMethods.exists(verificationMethod =>
        toPublicKey(verificationMethod).exists(publicKey => validateEncodedJwt(jwt, publicKey))
      )

    validatedVerificationMethodsIO
  }

  // TODO Implement other key types
  def toPublicKey(verificationMethod: VerificationMethod): Option[PublicKey] = {
    for {
      publicKeyJwk <- verificationMethod.publicKeyJwk
      curve <- publicKeyJwk.crv
      x <- publicKeyJwk.x.map(Base64URL.from)
      y <- publicKeyJwk.y.map(Base64URL.from)
      d <- publicKeyJwk.d.map(Base64URL.from)
    } yield new ECKey.Builder(Curve.parse(curve), x, y).d(d).build().toPublicKey
  }

}
