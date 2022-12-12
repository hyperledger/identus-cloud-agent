package io.iohk.atala.pollux.vc.jwt
import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*
import com.nimbusds.jose.util.Base64URL
import io.circe
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.iohk.atala.pollux.vc.jwt.JWTVerification.{extractPublicKey, validateEncodedJwt}
import io.iohk.atala.pollux.vc.jwt.schema.{SchemaResolver, SchemaValidator}
import net.reactivecore.cjs.validator.Violation
import net.reactivecore.cjs.{DocumentValidator, Loader}
import pdi.jwt.*
import zio.ZIO.none
import zio.prelude.*
import zio.{IO, NonEmptyChunk, Task, ZIO}

import java.security.spec.{ECParameterSpec, ECPublicKeySpec}
import java.security.{KeyPairGenerator, PublicKey}
import java.time.temporal.{Temporal, TemporalAmount, TemporalUnit}
import java.time.{Clock, Instant, ZonedDateTime}
import java.util
import scala.util.{Failure, Success, Try}

object JWTVerification {
  def validateEncodedJwt[T](jwt: JWT)(
      didResolver: DidResolver
  )(decoder: String => Validation[String, T])(issuerDidExtractor: T => String): IO[String, Validation[String, Unit]] = {
    val decodeJWT = Validation
      .fromTry(JwtCirce.decodeRawAll(jwt.value, JwtOptions(false, false, false)))
      .mapError(_.getMessage)

    val extractAlgorithm: Validation[String, JwtAlgorithm] =
      for {
        decodedJwtTask <- decodeJWT
        (header, _, _) = decodedJwtTask
        algorithm <- Validation
          .fromOptionWith("An algorithm must be specified in the header")(JwtCirce.parseHeader(header).algorithm)
      } yield algorithm

    val validatedIssuerDid: Validation[String, String] =
      for {
        decodedJwtTask <- decodeJWT
        (_, claim, _) = decodedJwtTask
        decodedClaim <- decoder(claim)
        extractIssuerDid = issuerDidExtractor(decodedClaim)
      } yield extractIssuerDid

    val loadDidDocument =
      ValidationUtils
        .foreach(
          validatedIssuerDid
            .map(validIssuerDid => resolve(validIssuerDid)(didResolver))
        )(identity)
        .map(b => b.flatten)

    loadDidDocument
      .map(validatedDidDocument => {
        for {
          results <- Validation.validateWith(validatedDidDocument, extractAlgorithm)((didDocument, algorithm) =>
            (didDocument, algorithm)
          )
          (didDocument, algorithm) = results
          verificationMethods <- extractVerificationMethods(didDocument, algorithm)
          validatedJwt <- validateEncodedJwt(jwt, verificationMethods)
        } yield validatedJwt
      })
  }

  def validateEncodedJwt(jwt: JWT, publicKey: PublicKey): Validation[String, Unit] =
    if JwtCirce.isValid(jwt.value, publicKey, JwtOptions(expiration = false, notBefore = false)) then Validation.unit
    else Validation.fail(s"Jwt[$jwt] not signed by $publicKey")

  def validateEncodedJwt(jwt: JWT, verificationMethods: IndexedSeq[VerificationMethod]): Validation[String, Unit] = {
    verificationMethods
      .map(verificationMethod => {
        for {
          publicKey <- extractPublicKey(verificationMethod)
          signatureValidation <- validateEncodedJwt(jwt, publicKey)
        } yield signatureValidation
      })
      .reduce((v1, v2) => v1.orElse(v2))
  }

  private def resolve(issuerDid: String)(didResolver: DidResolver): IO[String, Validation[String, DIDDocument]] = {
    didResolver
      .resolve(issuerDid)
      .map(
        _ match
          case (didResolutionSucceeded: DIDResolutionSucceeded) =>
            Validation.succeed(didResolutionSucceeded.didDocument)
          case (didResolutionFailed: DIDResolutionFailed) => Validation.fail(didResolutionFailed.error.toString)
      )
  }

  // TODO: Implement other verification relationship for JWT verification
  private def extractVerificationMethods(
      didDocument: DIDDocument,
      jwtAlgorithm: JwtAlgorithm
  ): Validation[String, IndexedSeq[VerificationMethod]] = {
    Validation
      .fromPredicateWith("No PublicKey to validate against found")(
        didDocument.assertionMethod.find(verification => verification.id == keyId)
      )(_.nonEmpty)
  }

  // TODO Implement other key types
  def extractPublicKey(verificationMethod: VerificationMethod): Validation[String, PublicKey] = {
    val maybePublicKey =
      for {
        publicKeyJwk <- verificationMethod.publicKeyJwk
        curve <- publicKeyJwk.crv
        x <- publicKeyJwk.x.map(Base64URL.from)
        y <- publicKeyJwk.y.map(Base64URL.from)
        d <- publicKeyJwk.d.map(Base64URL.from)
      } yield new ECKey.Builder(Curve.parse(curve), x, y).d(d).build().toPublicKey
    Validation.fromOptionWith("Unable to parse Public Key")(maybePublicKey)
  }
}
