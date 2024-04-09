package io.iohk.atala.pollux.core.service.verification

import zio.{IO, *}

class VcVerificationServiceImpl extends VcVerificationService {
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

  private case class VcVerificationOutcome(verification: VcVerification, success: Boolean)
  private def verify(
      credential: String,
      verification: VcVerification
  ): IO[VcVerificationServiceError, VcVerificationOutcome] = {
    verification match {
      case VcVerification.SchemaCheck => verifySchema(credential)
      case _ => ZIO.fail(VcVerificationServiceError.UnexpectedError(s"Unsupported Verification $verification"))
    }
  }

  private def verifySchema(credential: String): IO[VcVerificationServiceError, VcVerificationOutcome] = {
    ZIO.succeed(VcVerificationOutcome(verification = VcVerification.SchemaCheck, success = true))
  }
}

object VcVerificationServiceImpl {
  val layer: ULayer[VcVerificationService] =
    ZLayer.succeed(VcVerificationServiceImpl())
}
