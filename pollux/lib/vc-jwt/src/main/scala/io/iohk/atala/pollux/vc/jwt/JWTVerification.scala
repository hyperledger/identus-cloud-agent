package io.iohk.atala.pollux.vc.jwt
import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*
import com.nimbusds.jose.util.Base64URL
import io.circe
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.iohk.atala.pollux.vc.jwt.schema.{SchemaResolver, SchemaValidator}
import net.reactivecore.cjs.validator.Violation
import net.reactivecore.cjs.{DocumentValidator, Loader}
import pdi.jwt.*
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
  )(decoder: String => IO[String, T])(issuerDidExtractor: T => String): IO[String, Boolean] = {
    val decodeJWT = ZIO
      .fromTry(JwtCirce.decodeRawAll(jwt.value, JwtOptions(false, false, false)))
      .mapError(_.getMessage)

    val extractAlgorithm =
      for {
        decodedJwtTask <- decodeJWT
        (header, _, _) = decodedJwtTask
        algorithm <- Validation
          .fromOptionWith("An algorithm must be specified in the header")(JwtCirce.parseHeader(header).algorithm)
          .toZIO
      } yield algorithm

    val loadDidDocument =
      for {
        decodedJwtTask <- decodeJWT
        (_, claim, _) = decodedJwtTask
        decodedClaim <- decoder(claim)
        extractIssuerDid = issuerDidExtractor(decodedClaim)
        resolvedDidDocument <- resolve(extractIssuerDid)(didResolver)
      } yield resolvedDidDocument

    for {
      results <- loadDidDocument validatePar extractAlgorithm
      (didDocument, algorithm) = results
      verificationMethods <- extractVerificationMethods(didDocument, algorithm)
    } yield validateEncodedJwt(jwt, verificationMethods)
  }

  def validateEncodedJwt(jwt: JWT, publicKey: PublicKey): Boolean =
    JwtCirce.isValid(jwt.value, publicKey)

  def validateEncodedJwt(jwt: JWT, verificationMethods: IndexedSeq[VerificationMethod]): Boolean = {
    verificationMethods.exists(verificationMethod =>
      toPublicKey(verificationMethod).exists(publicKey => validateEncodedJwt(jwt, publicKey))
    )
  }

  private def resolve(issuerDid: String)(didResolver: DidResolver): IO[String, DIDDocument] = {
    didResolver
      .resolve(issuerDid)
      .flatMap(
        _ match
          case (didResolutionSucceeded: DIDResolutionSucceeded) =>
            ZIO.succeed(didResolutionSucceeded.didDocument)
          case (didResolutionFailed: DIDResolutionFailed) => ZIO.fail(didResolutionFailed.error.toString)
      )
  }

  private def extractVerificationMethods(
      didDocument: DIDDocument,
      jwtAlgorithm: JwtAlgorithm
  ): IO[String, IndexedSeq[VerificationMethod]] = {
    Validation
      .fromPredicateWith("No PublicKey to validate against found")(
        didDocument.verificationMethod.filter(verification => verification.`type` == jwtAlgorithm.name)
      )(_.nonEmpty)
      .toZIO
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
