package io.iohk.atala.pollux.core.service.verification

import io.iohk.atala.pollux.core.model.schema.CredentialSchema
import io.iohk.atala.pollux.core.service.URIDereferencer
import io.iohk.atala.pollux.vc.jwt.{DidResolver, JWT, JWTVerification, JwtCredential}
import sttp.tapir.Schema
import zio.{IO, *}

class VcVerificationServiceImpl(didResolver: DidResolver, uriDereferencer: URIDereferencer)
    extends VcVerificationService {
  override def verify(
      vcVerificationRequests: List[VcVerificationRequest]
  ): IO[VcVerificationServiceError, List[VcVerificationResult]] = {
    ZIO.collectAll(
      vcVerificationRequests.map(vcVerificationRequest =>
        verify(vcVerificationRequest.credential, vcVerificationRequest.verification, vcVerificationRequest.parameter)
      )
    )
  }

  private def verify(
      credential: String,
      verification: VcVerification,
      maybeParameter: Option[VcVerificationParameter]
  ): IO[VcVerificationServiceError, VcVerificationResult] = {
    (verification, maybeParameter) match {
      case (VcVerification.SchemaCheck, None)                           => verifySchema(credential)
      case (VcVerification.SignatureVerification, None)                 => verifySignature(credential)
      case (VcVerification.ExpirationCheck, None)                       => verifyExpiration(credential)
      case (VcVerification.NotBeforeCheck, None)                        => verifyNotBefore(credential)
      case (VcVerification.AlgorithmVerification, None)                 => verifyAlgorithm(credential)
      case (VcVerification.IssuerIdentification, None)                  => verifyIssuerIdentification(credential)
      case (VcVerification.SubjectVerification, None)                   => verifySubjectVerification(credential)
      case (VcVerification.SemanticCheckOfClaims, None)                 => verifySemanticCheckOfClaims(credential)
      case (VcVerification.AudienceCheck, Some(AudienceParameter(aud))) => verifyAudienceCheck(credential, aud)
      case _ =>
        ZIO.fail(
          VcVerificationServiceError.UnexpectedError(
            s"Unsupported Verification:$verification and Parameters:$maybeParameter"
          )
        )
    }
  }

  private def verifySchema(credential: String): IO[VcVerificationServiceError, VcVerificationResult] = {
    val result =
      for {
        decodedJwt <-
          JwtCredential
            .decodeJwt(JWT(credential))
            .mapError(error => VcVerificationServiceError.UnexpectedError(s"Unable decode JWT: $error"))
        credentialSchema <-
          ZIO
            .fromOption(decodedJwt.maybeCredentialSchema)
            .mapError(error => VcVerificationServiceError.UnexpectedError(s"Missing Credential Schema: $error"))
        result <- CredentialSchema
          .validSchemaValidator(
            credentialSchema.id,
            uriDereferencer
          )
          .mapError(error => VcVerificationServiceError.UnexpectedError(s"Schema Validator Failed: $error"))
      } yield result

    result
      .as(
        VcVerificationResult(
          credential = credential,
          verification = VcVerification.SchemaCheck,
          success = true
        )
      )
      .catchAll(_ =>
        ZIO.succeed(
          VcVerificationResult(
            credential = credential,
            verification = VcVerification.SchemaCheck,
            success = false
          )
        )
      )
  }

  private def verifySubjectVerification(credential: String): IO[VcVerificationServiceError, VcVerificationResult] = {
    val result =
      for {
        decodedJwt <-
          JwtCredential
            .decodeJwt(JWT(credential))
            .mapError(error => VcVerificationServiceError.UnexpectedError(s"Unable decode JWT: $error"))
        credentialSchema <-
          ZIO
            .fromOption(decodedJwt.maybeCredentialSchema)
            .mapError(error => VcVerificationServiceError.UnexpectedError(s"Missing Credential Schema: $error"))
        result <- CredentialSchema
          .validateJWTCredentialSubject(
            credentialSchema.id,
            decodedJwt.credentialSubject.noSpaces,
            uriDereferencer
          )
          .mapError(error =>
            VcVerificationServiceError.UnexpectedError(s"JWT Credential Subject Validation Failed: $error")
          )
      } yield result

    result
      .as(
        VcVerificationResult(
          credential = credential,
          verification = VcVerification.SubjectVerification,
          success = true
        )
      )
      .catchAll(_ =>
        ZIO.succeed(
          VcVerificationResult(
            credential = credential,
            verification = VcVerification.SubjectVerification,
            success = false
          )
        )
      )
  }

  private def verifySignature(credential: String): IO[VcVerificationServiceError, VcVerificationResult] = {
    JwtCredential
      .validateEncodedJWT(JWT(credential))(didResolver)
      .mapError(error => VcVerificationServiceError.UnexpectedError(error))
      .map(validation =>
        VcVerificationResult(
          credential = credential,
          verification = VcVerification.SignatureVerification,
          success = validation
            .map(_ => true)
            .getOrElse(false)
        )
      )
  }

  private def verifyExpiration(credential: String): IO[VcVerificationServiceError, VcVerificationResult] = {
    ZIO.succeed(
      VcVerificationResult(
        credential = credential,
        verification = VcVerification.ExpirationCheck,
        success = JwtCredential
          .validateExpiration(JWT(credential))
          .map(_ => true)
          .getOrElse(false)
      )
    )
  }

  private def verifyNotBefore(credential: String): IO[VcVerificationServiceError, VcVerificationResult] = {
    ZIO.succeed(
      VcVerificationResult(
        credential = credential,
        verification = VcVerification.NotBeforeCheck,
        success = JwtCredential
          .validateNotBefore(JWT(credential))
          .map(_ => true)
          .getOrElse(false)
      )
    )
  }

  private def verifyAlgorithm(credential: String): IO[VcVerificationServiceError, VcVerificationResult] = {
    ZIO.succeed(
      VcVerificationResult(
        credential = credential,
        verification = VcVerification.AlgorithmVerification,
        success = JWTVerification
          .validateAlgorithm(JWT(credential))
          .map(_ => true)
          .getOrElse(false)
      )
    )
  }

  private def verifyIssuerIdentification(credential: String): IO[VcVerificationServiceError, VcVerificationResult] = {
    JwtCredential
      .validateIssuerJWT(JWT(credential))(didResolver)
      .mapError(error => VcVerificationServiceError.UnexpectedError(error))
      .map(validation =>
        VcVerificationResult(
          credential = credential,
          verification = VcVerification.IssuerIdentification,
          success = validation
            .map(_ => true)
            .getOrElse(false)
        )
      )
  }

  private def verifySemanticCheckOfClaims(credential: String): IO[VcVerificationServiceError, VcVerificationResult] = {
    val result =
      for {
        decodedJwt <-
          JwtCredential
            .decodeJwt(JWT(credential))
            .mapError(error => VcVerificationServiceError.UnexpectedError(s"Unable decode JWT: $error"))
      } yield decodedJwt

    result
      .as(
        VcVerificationResult(
          credential = credential,
          verification = VcVerification.SubjectVerification,
          success = true
        )
      )
      .catchAll(_ =>
        ZIO.succeed(
          VcVerificationResult(
            credential = credential,
            verification = VcVerification.SubjectVerification,
            success = false
          )
        )
      )
  }

  private def verifyAudienceCheck(
      credential: String,
      aud: String
  ): IO[VcVerificationServiceError, VcVerificationResult] = {
    ZIO.succeed(
      VcVerificationResult(
        credential = credential,
        verification = VcVerification.SubjectVerification,
        success = true
      )
    )
  }
}

object VcVerificationServiceImpl {
  val layer: URLayer[DidResolver & URIDereferencer, VcVerificationService] =
    ZLayer.fromFunction(VcVerificationServiceImpl(_, _))
}
