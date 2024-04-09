package io.iohk.atala.verification.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.pollux.core.service.verification.{VcVerification, VcVerificationRequest, VcVerificationService}
import io.iohk.atala.verification.controller
import zio.*

class VcVerificationControllerImpl(vcVerificationService: VcVerificationService) extends VcVerificationController {

  override def verify(
      requests: List[controller.http.VcVerificationRequest]
  )(implicit rc: RequestContext): IO[ErrorResponse, List[controller.http.VcVerificationResponse]] = {
    val serviceRequests =
      requests.map(request => {
        val verifications =
          if (request.verifications.isEmpty)
            VcVerification.values.toList
          else
            request.verifications

        VcVerificationRequest(
          credential = request.credential,
          verifications = verifications
        )
      })
    for {
      results <-
        vcVerificationService
          .verify(serviceRequests)
          .mapError(error => VcVerificationController.toHttpError(error))
    } yield results.map(result =>
      controller.http.VcVerificationResponse(
        result.credential,
        result.checks,
        result.successfulChecks,
        result.failedChecks,
        result.failedAsWarningChecks
      )
    )
  }
}

object VcVerificationControllerImpl {
  val layer: URLayer[VcVerificationService, VcVerificationController] =
    ZLayer.fromFunction(VcVerificationControllerImpl(_))
}
