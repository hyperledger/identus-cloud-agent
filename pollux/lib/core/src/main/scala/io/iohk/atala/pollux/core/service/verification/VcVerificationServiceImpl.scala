package io.iohk.atala.pollux.core.service.verification

import io.iohk.atala.pollux.core.model.schema.CredentialSchema
import io.iohk.atala.pollux.core.service.URIDereferencer
import io.iohk.atala.pollux.vc.jwt.{DidResolver, JWT, JWTVerification, JwtCredential}
import zio.{IO, *}

class VcVerificationServiceImpl(didResolver: DidResolver, uriDereferencer: URIDereferencer)
    extends VcVerificationService {
  override def verify(
      vcVerificationRequests: List[VcVerificationRequest]
  ): IO[VcVerificationServiceError, List[VcVerificationResult]] = {
    vcVerificationRequests.map(vcVerificationRequest =>
      val verificationOutcomesZIO = ZIO.collectAll(
        vcVerificationRequest.verifications
          .map(verification => verify(vcVerificationRequest.credential, verification))
      )

      verificationOutcomesZIO.map(verificationOutcomes => {
        val successfulChecks = verificationOutcomes.filter(_.success).map(_.verification)

        val failedVerifications = verificationOutcomes.filterNot(_.success).map(_.verification)

        val failedAsErrorChecks =
          failedVerifications.filter(verification => verification.failureType == VcVerificationFailureType.ERROR)

        val failedAsWarningChecks =
          failedVerifications.filter(verification => verification.failureType == VcVerificationFailureType.WARN)

        VcVerificationResult(
          credential = vcVerificationRequest.credential,
          checks = vcVerificationRequest.verifications,
          successfulChecks = successfulChecks,
          failedChecks = failedAsErrorChecks,
          failedAsWarningChecks = failedAsWarningChecks
        )
      })
    )
    ZIO.succeed(List.empty)
  }

  private def verify(
      credential: String,
      verification: VcVerification
  ): IO[VcVerificationServiceError, VcVerificationOutcome] = {
    verification match {
      case VcVerification.SchemaCheck           => verifySchema(credential)
      case VcVerification.SignatureVerification => verifySignature(credential)
      case VcVerification.ExpirationCheck       => verifyExpiration(credential)
      case VcVerification.NotBeforeCheck        => verifyNotBefore(credential)
      case VcVerification.AlgorithmVerification => verifyAlgorithm(credential)
      case VcVerification.IssuerIdentification  => verifyIssuerIdentification(credential)
      case VcVerification.SubjectVerification   => verifySubjectVerification(credential)
      case VcVerification.SemanticCheckOfClaims => verifySemanticCheckOfClaims(credential)
      case _ => ZIO.fail(VcVerificationServiceError.UnexpectedError(s"Unsupported Verification $verification"))
    }
  }

  private def verifySchema(credential: String): IO[VcVerificationServiceError, VcVerificationOutcome] = {
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
        VcVerificationOutcome(
          verification = VcVerification.SchemaCheck,
          success = true
        )
      )
      .catchAll(_ =>
        ZIO.succeed(
          VcVerificationOutcome(
            verification = VcVerification.SchemaCheck,
            success = false
          )
        )
      )
  }

  private def verifySubjectVerification(credential: String): IO[VcVerificationServiceError, VcVerificationOutcome] = {
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
        VcVerificationOutcome(
          verification = VcVerification.SubjectVerification,
          success = true
        )
      )
      .catchAll(_ =>
        ZIO.succeed(
          VcVerificationOutcome(
            verification = VcVerification.SubjectVerification,
            success = false
          )
        )
      )
  }

  private def verifySignature(credential: String): IO[VcVerificationServiceError, VcVerificationOutcome] = {
    JwtCredential
      .validateEncodedJWT(JWT(credential))(didResolver)
      .mapError(error => VcVerificationServiceError.UnexpectedError(error))
      .map(validation =>
        VcVerificationOutcome(
          verification = VcVerification.SignatureVerification,
          success = validation
            .map(_ => true)
            .getOrElse(false)
        )
      )
  }

  private def verifyExpiration(credential: String): IO[VcVerificationServiceError, VcVerificationOutcome] = {
    ZIO.succeed(
      VcVerificationOutcome(
        verification = VcVerification.ExpirationCheck,
        success = JwtCredential
          .validateExpiration(JWT(credential))
          .map(_ => true)
          .getOrElse(false)
      )
    )
  }

  private def verifyNotBefore(credential: String): IO[VcVerificationServiceError, VcVerificationOutcome] = {
    ZIO.succeed(
      VcVerificationOutcome(
        verification = VcVerification.NotBeforeCheck,
        success = JwtCredential
          .validateNotBefore(JWT(credential))
          .map(_ => true)
          .getOrElse(false)
      )
    )
  }

  private def verifyAlgorithm(credential: String): IO[VcVerificationServiceError, VcVerificationOutcome] = {
    ZIO.succeed(
      VcVerificationOutcome(
        verification = VcVerification.AlgorithmVerification,
        success = JWTVerification
          .validateAlgorithm(JWT(credential))
          .map(_ => true)
          .getOrElse(false)
      )
    )
  }

  private def verifyIssuerIdentification(credential: String): IO[VcVerificationServiceError, VcVerificationOutcome] = {
    JwtCredential
      .validateIssuerJWT(JWT(credential))(didResolver)
      .mapError(error => VcVerificationServiceError.UnexpectedError(error))
      .map(validation =>
        VcVerificationOutcome(
          verification = VcVerification.IssuerIdentification,
          success = validation
            .map(_ => true)
            .getOrElse(false)
        )
      )
  }

  private def verifySemanticCheckOfClaims(credential: String): IO[VcVerificationServiceError, VcVerificationOutcome] = {
    val result =
      for {
        decodedJwt <-
          JwtCredential
            .decodeJwt(JWT(credential))
            .mapError(error => VcVerificationServiceError.UnexpectedError(s"Unable decode JWT: $error"))
      } yield decodedJwt

    result
      .as(
        VcVerificationOutcome(
          verification = VcVerification.SubjectVerification,
          success = true
        )
      )
      .catchAll(_ =>
        ZIO.succeed(
          VcVerificationOutcome(
            verification = VcVerification.SubjectVerification,
            success = false
          )
        )
      )
  }
}

object VcVerificationServiceImpl {
  val layer: URLayer[DidResolver & URIDereferencer, VcVerificationService] =
    ZLayer.fromFunction(VcVerificationServiceImpl(_, _))
}
