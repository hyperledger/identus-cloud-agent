package io.iohk.atala.pollux.vc.jwt
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import com.nimbusds.jwt.SignedJWT
import io.circe
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.pollux.vc.jwt.JWTVerification.{extractPublicKey, validateEncodedJwt}
import io.iohk.atala.pollux.vc.jwt.schema.{SchemaResolver, SchemaValidator}
import net.reactivecore.cjs.validator.Violation
import net.reactivecore.cjs.{DocumentValidator, Loader}
import pdi.jwt.*
import zio.ZIO.none
import zio.prelude.*
import zio.{IO, NonEmptyChunk, Task, ZIO}

import java.security.interfaces.ECPublicKey
import java.security.spec.{ECParameterSpec, ECPublicKeySpec}
import java.security.{KeyPairGenerator, PublicKey}
import java.time.temporal.{Temporal, TemporalAmount, TemporalUnit}
import java.time.{Clock, Instant, ZonedDateTime}
import java.util
import scala.util.{Failure, Success, Try}

object JWTVerification {
  // JWT algo <-> publicKey type mapping reference
  // https://github.com/decentralized-identity/did-jwt/blob/8b3655097a1382934cabdf774d580e6731a636b1/src/JWT.ts#L146
  val SUPPORT_PUBLIC_KEY_TYPES: Map[String, Set[String]] = Map(
    "ES256K" -> Set("EcdsaSecp256k1VerificationKey2019", "JsonWebKey2020"),
    "ES256" -> Set("ES256") // TODO: Only use valid type (added just for compatibility in the Demo code)
  )

  def validateEncodedJwt[T](jwt: JWT, proofPurpose: Option[VerificationRelationship] = None)(
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
          verificationMethods <- extractVerificationMethods(didDocument, algorithm, proofPurpose)
          validatedJwt <- validateEncodedJwt(jwt, verificationMethods)
        } yield validatedJwt
      })
  }

  def toECDSAVerifier(publicKey: PublicKey): JWSVerifier = {
    val verifier: JWSVerifier = publicKey match {
      case key: ECPublicKey => ECDSAVerifier(key)
      case key              => throw Exception(s"unsupported public-key type: ${key.getClass.getCanonicalName}")
    }
    verifier.getJCAContext.setProvider(BouncyCastleProviderSingleton.getInstance)
    verifier
  }

  def validateEncodedJwt(jwt: JWT, publicKey: PublicKey): Validation[String, Unit] = {
    Try {
      val parsedJwt = SignedJWT.parse(jwt.value)
      // TODO Implement other key types
      val verifier = toECDSAVerifier(publicKey)
      parsedJwt.verify(verifier)
    } match {
      case Failure(exception) => Validation.fail(s"Jwt[$jwt] verification pre-process failed: ${exception.getMessage}")
      case Success(isValid) =>
        if isValid then Validation.unit else Validation.fail(s"Jwt[$jwt] not singed by $publicKey")
    }
  }

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

  private def extractVerificationMethods(
      didDocument: DIDDocument,
      jwtAlgorithm: JwtAlgorithm,
      proofPurpose: Option[VerificationRelationship]
  ): Validation[String, IndexedSeq[VerificationMethod]] = {
    val publicKeysToCheck: Vector[VerificationMethodOrRef] = proofPurpose.fold(didDocument.verificationMethod) {
      case VerificationRelationship.Authentication       => didDocument.authentication
      case VerificationRelationship.AssertionMethod      => didDocument.assertionMethod
      case VerificationRelationship.KeyAgreement         => didDocument.keyAgreement
      case VerificationRelationship.CapabilityInvocation => didDocument.capabilityInvocation
      case VerificationRelationship.CapabilityDelegation => didDocument.capabilityDelegation
    }
    // FIXME
    // To be fully compliant, key extraction MUST follow the referenced URI which
    // might not be in the same DID document. For now, this only performs lookup within
    // the same DID document which is what Prism DID currently support.
    val dereferencedKeysToCheck: Vector[VerificationMethod] = {
      val (referenced, embedded) = publicKeysToCheck.partitionMap[String, VerificationMethod] {
        case uri: String            => Left(uri)
        case pk: VerificationMethod => Right(pk)
      }
      val keySet = referenced.toSet
      embedded ++ didDocument.verificationMethod.filter(pk => keySet.contains(pk.id))
    }
    Validation
      .fromPredicateWith("No PublicKey to validate against found")(
        dereferencedKeysToCheck.filter { verification =>
          val supportPublicKeys = SUPPORT_PUBLIC_KEY_TYPES.getOrElse(jwtAlgorithm.name, Set.empty)
          supportPublicKeys.contains(verification.`type`)
        }
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
      } yield new ECKey.Builder(Curve.parse(curve), x, y).build().toPublicKey
    Validation.fromOptionWith("Unable to parse Public Key")(maybePublicKey)
  }
}
