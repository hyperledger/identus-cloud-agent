package io.iohk.atala.pollux.vc.jwt

import io.circe
import io.circe.*
import io.circe.Decoder.Result
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import io.iohk.atala.castor.core.model.did.VerificationRelationship
import pdi.jwt.{Jwt, JwtCirce, JwtOptions}
import zio.*
import zio.prelude.*

import java.security.{KeyPairGenerator, PublicKey}
import java.time.temporal.TemporalAmount
import java.time.{Clock, Instant, ZonedDateTime}
import scala.util.{Failure, Success, Try}

sealed trait VerifiablePresentationPayload

case class Prover(did: DID, signer: Signer, publicKey: PublicKey)

case class W3cVerifiablePresentationPayload(payload: W3cPresentationPayload, proof: Proof)
    extends Verifiable(proof),
      VerifiablePresentationPayload

case class JwtVerifiablePresentationPayload(jwt: JWT) extends VerifiablePresentationPayload

sealed trait PresentationPayload(
    `@context`: IndexedSeq[String],
    `type`: IndexedSeq[String],
    verifiableCredential: IndexedSeq[VerifiableCredentialPayload],
    iss: String,
    maybeNbf: Option[Instant],
    aud: IndexedSeq[String],
    maybeExp: Option[Instant],
    maybeJti: Option[String],
    maybeNonce: Option[String]
) {
  def toJwtPresentationPayload: JwtPresentationPayload =
    JwtPresentationPayload(
      iss = iss,
      vp = JwtVp(
        `@context` = `@context`,
        `type` = `type`,
        verifiableCredential = verifiableCredential
      ),
      maybeNbf = maybeNbf,
      aud = aud,
      maybeExp = maybeExp,
      maybeJti = maybeJti,
      maybeNonce = maybeNonce
    )

  def toW3CPresentationPayload: W3cPresentationPayload =
    W3cPresentationPayload(
      `@context` = `@context`.distinct,
      maybeId = maybeJti,
      `type` = `type`.distinct,
      verifiableCredential = verifiableCredential,
      holder = iss,
      verifier = aud,
      maybeIssuanceDate = maybeNbf,
      maybeExpirationDate = maybeExp,
      maybeNonce = maybeNonce
    )
}

case class W3cPresentationPayload(
    `@context`: IndexedSeq[String],
    maybeId: Option[String],
    `type`: IndexedSeq[String],
    verifiableCredential: IndexedSeq[VerifiableCredentialPayload],
    holder: String,
    verifier: IndexedSeq[String],
    maybeIssuanceDate: Option[Instant],
    maybeExpirationDate: Option[Instant],

    /** Not part of W3C Presentation but included to preserve in case of conversion from JWT. */
    maybeNonce: Option[String] = Option.empty
) extends PresentationPayload(
      `@context` = `@context`.distinct,
      `type` = `type`.distinct,
      maybeJti = maybeId,
      verifiableCredential = verifiableCredential,
      aud = verifier,
      iss = holder,
      maybeNbf = maybeIssuanceDate,
      maybeExp = maybeExpirationDate,
      maybeNonce = maybeNonce
    )

case class JwtVp(
    `@context`: IndexedSeq[String],
    `type`: IndexedSeq[String],
    verifiableCredential: IndexedSeq[VerifiableCredentialPayload]
)

case class JwtPresentationPayload(
    iss: String,
    vp: JwtVp,
    maybeNbf: Option[Instant],
    aud: IndexedSeq[String],
    maybeExp: Option[Instant],
    maybeJti: Option[String],
    maybeNonce: Option[String]
) extends PresentationPayload(
      iss = iss,
      `@context` = vp.`@context`,
      `type` = vp.`type`,
      verifiableCredential = vp.verifiableCredential,
      maybeNbf = maybeNbf,
      aud = aud,
      maybeExp = maybeExp,
      maybeJti = maybeJti,
      maybeNonce = maybeNonce
    )

object PresentationPayload {

  object Implicits {

    import CredentialPayload.Implicits.*
    import InstantDecoderEncoder.*
    import Proof.Implicits.*

    implicit val w3cPresentationPayloadEncoder: Encoder[W3cPresentationPayload] =
      (w3cPresentationPayload: W3cPresentationPayload) =>
        Json
          .obj(
            ("@context", w3cPresentationPayload.`@context`.asJson),
            ("id", w3cPresentationPayload.maybeId.asJson),
            ("type", w3cPresentationPayload.`type`.asJson),
            ("verifiableCredential", w3cPresentationPayload.verifiableCredential.asJson),
            ("holder", w3cPresentationPayload.holder.asJson),
            ("verifier", w3cPresentationPayload.verifier.asJson),
            ("issuanceDate", w3cPresentationPayload.maybeIssuanceDate.asJson),
            ("expirationDate", w3cPresentationPayload.maybeExpirationDate.asJson)
          )
          .deepDropNullValues
          .dropEmptyValues

    implicit val jwtVpEncoder: Encoder[JwtVp] =
      (jwtVp: JwtVp) =>
        Json
          .obj(
            ("@context", jwtVp.`@context`.asJson),
            ("type", jwtVp.`type`.asJson),
            ("verifiableCredential", jwtVp.verifiableCredential.asJson)
          )
          .deepDropNullValues
          .dropEmptyValues

    implicit val jwtPresentationPayloadEncoder: Encoder[JwtPresentationPayload] =
      (jwtPresentationPayload: JwtPresentationPayload) =>
        Json
          .obj(
            ("iss", jwtPresentationPayload.iss.asJson),
            ("vp", jwtPresentationPayload.vp.asJson),
            ("nbf", jwtPresentationPayload.maybeNbf.asJson),
            ("aud", jwtPresentationPayload.aud.asJson),
            ("exp", jwtPresentationPayload.maybeExp.asJson),
            ("jti", jwtPresentationPayload.maybeJti.asJson),
            ("nonce", jwtPresentationPayload.maybeNonce.asJson)
          )
          .deepDropNullValues
          .dropEmptyValues

    implicit val w3cPresentationPayloadDecoder: Decoder[W3cPresentationPayload] =
      (c: HCursor) =>
        for {
          `@context` <- c
            .downField("@context")
            .as[IndexedSeq[String]]
            .orElse(c.downField("@context").as[String].map(IndexedSeq(_)))
          maybeId <- c.downField("id").as[Option[String]]
          `type` <- c
            .downField("type")
            .as[IndexedSeq[String]]
            .orElse(c.downField("type").as[String].map(IndexedSeq(_)))
          holder <- c.downField("holder").as[String]
          verifiableCredential <- c
            .downField("verifiableCredential")
            .as[Option[VerifiableCredentialPayload]]
            .map(_.iterator.toIndexedSeq)
            .orElse(
              c.downField("verifiableCredential")
                .as[Option[IndexedSeq[VerifiableCredentialPayload]]]
                .map(_.iterator.toIndexedSeq.flatten)
            )
          verifier <- c
            .downField("verifier")
            .as[Option[String]]
            .map(_.iterator.toIndexedSeq)
            .orElse(c.downField("verifier").as[Option[IndexedSeq[String]]].map(_.iterator.toIndexedSeq.flatten))
          maybeIssuanceDate <- c.downField("issuanceDate").as[Option[Instant]]
          maybeExpirationDate <- c.downField("expirationDate").as[Option[Instant]]
        } yield {
          W3cPresentationPayload(
            `@context` = `@context`.distinct,
            maybeId = maybeId,
            `type` = `type`.distinct,
            verifiableCredential = verifiableCredential.distinct,
            holder = holder,
            verifier = verifier.distinct,
            maybeIssuanceDate = maybeIssuanceDate,
            maybeExpirationDate = maybeExpirationDate,
            maybeNonce = Option.empty
          )
        }

    implicit val jwtVpDecoder: Decoder[JwtVp] =
      (c: HCursor) =>
        for {
          `@context` <- c
            .downField("@context")
            .as[IndexedSeq[String]]
            .orElse(c.downField("@context").as[String].map(IndexedSeq(_)))
          `type` <- c
            .downField("type")
            .as[IndexedSeq[String]]
            .orElse(c.downField("type").as[String].map(IndexedSeq(_)))
          maybeVerifiableCredential <- c
            .downField("verifiableCredential")
            .as[Option[IndexedSeq[VerifiableCredentialPayload]]]
        } yield {
          JwtVp(
            `@context` = `@context`.distinct,
            `type` = `type`.distinct,
            verifiableCredential = maybeVerifiableCredential.toIndexedSeq.flatten
          )
        }

    implicit val JwtPresentationPayloadDecoder: Decoder[JwtPresentationPayload] =
      (c: HCursor) =>
        for {
          iss <- c.downField("iss").as[String]
          vp <- c.downField("vp").as[JwtVp]
          maybeNbf <- c.downField("nbf").as[Option[Instant]]
          aud <- c
            .downField("aud")
            .as[Option[String]]
            .map(_.iterator.toIndexedSeq)
            .orElse(c.downField("aud").as[Option[IndexedSeq[String]]].map(_.iterator.toIndexedSeq.flatten))
          maybeExp <- c.downField("exp").as[Option[Instant]]
          maybeJti <- c.downField("jti").as[Option[String]]
          maybeNonce <- c.downField("nonce").as[Option[String]]
        } yield {
          JwtPresentationPayload(
            iss = iss,
            vp = vp,
            maybeNbf = maybeNbf,
            aud = aud.distinct,
            maybeExp = maybeExp,
            maybeJti = maybeJti,
            maybeNonce = maybeNonce
          )
        }

    implicit val w3cVerifiablePresentationPayloadDecoder: Decoder[W3cVerifiablePresentationPayload] =
      (c: HCursor) =>
        for {
          payload <- c.as[W3cPresentationPayload]
          proof <- c.downField("proof").as[Proof]
        } yield {
          W3cVerifiablePresentationPayload(
            payload = payload,
            proof = proof
          )
        }

    implicit val jwtVerifiablePresentationPayloadDecoder: Decoder[JwtVerifiablePresentationPayload] =
      (c: HCursor) =>
        for {
          jwt <- c.as[String]
        } yield {
          JwtVerifiablePresentationPayload(
            jwt = JWT(jwt)
          )
        }

    implicit val verifiablePresentationPayloadDecoder: Decoder[VerifiablePresentationPayload] =
      jwtVerifiablePresentationPayloadDecoder.or(
        w3cVerifiablePresentationPayloadDecoder.asInstanceOf[Decoder[VerifiablePresentationPayload]]
      )
  }
}

object JwtPresentation {

  import PresentationPayload.Implicits.*

  def encodeJwt(payload: JwtPresentationPayload, issuer: Issuer): JWT = issuer.signer.encode(payload.asJson)

  def toEncodeW3C(payload: W3cPresentationPayload, issuer: Issuer): W3cVerifiablePresentationPayload = {
    W3cVerifiablePresentationPayload(
      payload = payload,
      proof = Proof(
        `type` = "JwtProof2020",
        jwt = issuer.signer.encode(payload.asJson)
      )
    )
  }

  def toEncodedJwt(payload: W3cPresentationPayload, issuer: Issuer): JWT =
    encodeJwt(payload.toJwtPresentationPayload, issuer)

  def decodeJwt(jwt: JWT): Try[JwtPresentationPayload] = {
    JwtCirce
      .decodeRaw(jwt.value, JwtOptions(signature = false, expiration = false, notBefore = false))
      .flatMap(decode[JwtPresentationPayload](_).toTry)
  }

  def decodeJwt(jwt: JWT, publicKey: PublicKey): Try[JwtPresentationPayload] = {
    JwtCirce
      .decodeRaw(jwt.value, publicKey, JwtOptions(expiration = false, notBefore = false))
      .flatMap(decode[JwtPresentationPayload](_).toTry)
  }

  def validateEncodedJwt(jwt: JWT, publicKey: PublicKey): Validation[String, Unit] =
    JWTVerification.validateEncodedJwt(jwt, publicKey)

  def validateEncodedJWT(
      jwt: JWT,
      proofPurpose: Option[VerificationRelationship]
  )(didResolver: DidResolver): IO[String, Validation[String, Unit]] = {
    JWTVerification.validateEncodedJwt(jwt, proofPurpose)(didResolver: DidResolver)(claim =>
      Validation.fromEither(decode[JwtPresentationPayload](claim).left.map(_.toString))
    )(_.iss)
  }

  def validateEncodedW3C(
      jwt: JWT,
      proofPurpose: Option[VerificationRelationship]
  )(didResolver: DidResolver): IO[String, Validation[String, Unit]] = {
    JWTVerification.validateEncodedJwt(jwt, proofPurpose)(didResolver: DidResolver)(claim =>
      Validation.fromEither(decode[W3cPresentationPayload](claim).left.map(_.toString))
    )(_.holder)
  }

  def validateEnclosedCredentials(
      jwt: JWT,
      options: CredentialVerification.CredentialVerificationOptions
  )(didResolver: DidResolver)(implicit clock: Clock): IO[List[String], Validation[String, Unit]] = {
    val validateJwtPresentation = Validation.fromTry(decodeJwt(jwt)).mapError(_.toString)

    val credentialValidationZIO =
      ValidationUtils.foreach(
        validateJwtPresentation
          .map(validJwtPresentation => validateCredentials(validJwtPresentation, options)(didResolver)(clock))
      )(identity)

    credentialValidationZIO.map(validCredentialValidations => {
      for {
        credentialValidations <- validCredentialValidations
        _ <- Validation.validateAll(credentialValidations)
        success <- Validation.unit
      } yield success
    })
  }

  def validateCredentials(
      decodedJwtPresentation: JwtPresentationPayload,
      options: CredentialVerification.CredentialVerificationOptions
  )(didResolver: DidResolver)(implicit clock: Clock): ZIO[Any, List[String], IndexedSeq[Validation[String, Unit]]] = {
    ZIO.validatePar(decodedJwtPresentation.vp.verifiableCredential) { a =>
      CredentialVerification.verify(a, options)(didResolver)(clock)
    }
  }

  def validatePresentation(
      jwt: JWT,
      domain: String,
      challenge: String
  ): Validation[String, Unit] = {
    val validateJwtPresentation = Validation.fromTry(decodeJwt(jwt)).mapError(_.toString)
    for {
      decodeJwtPresentation <- validateJwtPresentation
      aud <- validateAudience(decodeJwtPresentation, domain)
      result <- validateNonce(decodeJwtPresentation, Some(challenge))
    } yield result

  }

  def validateNonce(
      decodedJwtPresentation: JwtPresentationPayload,
      nonce: Option[String]
  ): Validation[String, Unit] = {
    if (nonce != decodedJwtPresentation.maybeNonce) {
      Validation.fail(s"Challenge/Nonce dont match nonce=$nonce exp=${decodedJwtPresentation.maybeNonce}")
    } else Validation.unit
  }
  def validateAudience(
      decodedJwtPresentation: JwtPresentationPayload,
      domain: String
  ): Validation[String, Unit] = {
    if (!decodedJwtPresentation.aud.contains(domain)) {
      Validation.fail(s"domain/Audience dont match doamin=$domain, exp=${decodedJwtPresentation.aud}")
    } else Validation.unit
  }

  def verifyHolderBinding(jwt: JWT): Validation[String, Unit] = {
    import io.iohk.atala.pollux.vc.jwt.CredentialPayload.Implicits.*
    val decodeJWT = (jwt: JWT) =>
      Validation
        .fromTry(JwtCirce.decodeRaw(jwt.value, options = JwtOptions(false, false, false)))
        .mapError(_.getMessage)

    def validateCredentialSubjectId(
        vcList: IndexedSeq[VerifiableCredentialPayload],
        iss: String
    ): Validation[String, Unit] = {
      ZValidation
        .validateAll(
          vcList.map {
            case (w3cVerifiableCredentialPayload: W3cVerifiableCredentialPayload) =>
              val mayBeSubjectDid = w3cVerifiableCredentialPayload.payload.credentialSubject.hcursor
                .downField("id")
                .as[String]
                .toOption
              if (mayBeSubjectDid.contains(iss)) {
                Validation.unit
              } else
                Validation.fail(
                  s"holder DID ${iss} that signed the presentation must match the credentialSubject did ${mayBeSubjectDid}  in each of the attached credentials"
                )

            case (jwtVerifiableCredentialPayload: JwtVerifiableCredentialPayload) =>
              for {
                jwtCredentialDecoded <- decodeJWT(jwtVerifiableCredentialPayload.jwt)
                sssss = decode[JwtCredentialPayload](jwtCredentialDecoded)
                jwtCredentialPayload <- Validation
                  .fromEither(decode[JwtCredentialPayload](jwtCredentialDecoded))
                  .mapError(_.getMessage)
                mayBeSubjectDid = jwtCredentialPayload.maybeSub
                x <-
                  if (mayBeSubjectDid.contains(iss)) {
                    Validation.unit
                  } else
                    Validation.fail(
                      s"holder DID ${iss} that signed the presentation must match the credentialSubject did  ${mayBeSubjectDid}  in each of the attached credentials"
                    )
              } yield x
          }
        )
        .map(_ => ())
    }
    for {
      decodedJWT <- decodeJWT(jwt)
      jwtPresentationPayload <- Validation.fromEither(decode[JwtPresentationPayload](decodedJWT)).mapError(_.getMessage)
      result <- validateCredentialSubjectId(jwtPresentationPayload.vp.verifiableCredential, jwtPresentationPayload.iss)
    } yield result
  }

  def verifyDates(jwt: JWT, leeway: TemporalAmount)(implicit clock: Clock): Validation[String, Unit] = {
    val now = clock.instant()

    val decodeJWT =
      Validation
        .fromTry(JwtCirce.decodeRaw(jwt.value, options = JwtOptions(false, false, false)))
        .mapError(_.getMessage)

    def validateNbfNotAfterExp(maybeNbf: Option[Instant], maybeExp: Option[Instant]): Validation[String, Unit] = {
      val maybeResult =
        for {
          nbf <- maybeNbf
          exp <- maybeExp
        } yield {
          if (nbf.isAfter(exp))
            Validation.fail(s"Credential cannot expire before being in effect. nbf=$nbf exp=$exp")
          else Validation.unit
        }
      maybeResult.getOrElse(Validation.unit)
    }

    def validateNbf(maybeNbf: Option[Instant]): Validation[String, Unit] = {
      maybeNbf
        .map(nbf =>
          if (now.isBefore(nbf.minus(leeway)))
            Validation.fail(s"Credential is not yet in effect. now=$now nbf=$nbf leeway=$leeway")
          else Validation.unit
        )
        .getOrElse(Validation.unit)
    }

    def validateExp(maybeExp: Option[Instant]): Validation[String, Unit] = {
      maybeExp
        .map(exp =>
          if (now.isAfter(exp.plus(leeway)))
            Validation.fail(s"Verifiable Presentation has expired. now=$now exp=$exp leeway=$leeway")
          else Validation.unit
        )
        .getOrElse(Validation.unit)
    }

    for {
      decodedJWT <- decodeJWT
      jwtCredentialPayload <- Validation.fromEither(decode[JwtPresentationPayload](decodedJWT)).mapError(_.getMessage)
      maybeNbf = jwtCredentialPayload.maybeNbf
      maybeExp = jwtCredentialPayload.maybeExp
      result <- Validation.validateWith(
        validateNbfNotAfterExp(maybeNbf, maybeExp),
        validateNbf(maybeNbf),
        validateExp(maybeExp)
      )((l, _, _) => l)
    } yield result
  }

  /** Defines what to verify in a jwt presentation
    * @param verifySignature
    *   verifies signature using the resolved did.
    * @param verifyDates
    *   verifies issuance and expiration dates.
    * @param leeway
    *   defines the duration we should subtract from issuance date and add to expiration dates.
    * @param maybeCredentialOptions
    *   defines what to verify in the jwt credentials. If empty, credentials verification will be ignored.
    * @param maybeProofPurpose
    *   specifies the which type of public key to use in the resolved DidDocument. If empty, we will validate against
    *   all public key.
    */
  case class PresentationVerificationOptions(
      verifySignature: Boolean = true,
      verifyDates: Boolean = false,
      verifyHoldersBinding: Boolean = false,
      leeway: TemporalAmount = Duration.Zero,
      maybeCredentialOptions: Option[CredentialVerification.CredentialVerificationOptions] = None,
      maybeProofPurpose: Option[VerificationRelationship] = None
  )

  /** Verifies a jwt presentation.
    * @param jwt
    *   presentation to verify.
    * @param options
    *   defines what to verify.
    * @param didResolver
    *   is used to resolve the did.
    * @param clock
    *   is used to get current time.
    * @return
    *   the result of the validation.
    */
  def verify(jwt: JWT, options: PresentationVerificationOptions)(
      didResolver: DidResolver
  )(implicit clock: Clock): IO[List[String], Validation[String, Unit]] = {
    for {
      signatureValidation <-
        if (options.verifySignature) then
          validateEncodedJWT(jwt, options.maybeProofPurpose)(didResolver).mapError(List(_))
        else ZIO.succeed(Validation.unit)
      dateVerification <- ZIO.succeed(
        if (options.verifyDates) then verifyDates(jwt, options.leeway)(clock) else Validation.unit
      )
      verifyHoldersBinding <- ZIO.succeed(
        if (options.verifyHoldersBinding) then verifyHolderBinding(jwt) else Validation.unit
      )
      credentialVerification <-
        options.maybeCredentialOptions
          .map(credentialOptions => validateEnclosedCredentials(jwt, credentialOptions)(didResolver)(clock))
          .getOrElse(ZIO.succeed(Validation.unit))
    } yield Validation.validateWith(
      signatureValidation,
      dateVerification,
      credentialVerification,
      verifyHoldersBinding
    )((a, _, _, _) => a)
  }
}
