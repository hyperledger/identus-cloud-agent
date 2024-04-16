package io.iohk.atala.verification.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.pollux.core.service.verification.VcVerificationService
import io.iohk.atala.verification.controller
import zio.*

class VcVerificationControllerImpl(vcVerificationService: VcVerificationService) extends VcVerificationController {

  override def verify(
      requests: List[controller.http.VcVerificationRequest]
  )(implicit rc: RequestContext): IO[ErrorResponse, List[controller.http.VcVerificationResponse]] = {
    val result =
      ZIO.collectAll(requests.map(request => {
        val serviceRequests = controller.http.VcVerificationRequest.toService(request)
        for {
          results <-
            vcVerificationService
              .verify(serviceRequests)
              .mapError(error => VcVerificationController.toHttpError(error))
        } yield controller.http.VcVerificationResponse(
          request.credential,
          results.map(result => controller.http.VcVerificationResult.toService(result))
        )
      }))
    ZIO.succeed(List.empty)
  }
}

object VcVerificationControllerImpl {
  val layer: URLayer[VcVerificationService, VcVerificationController] =
    ZLayer.fromFunction(VcVerificationControllerImpl(_))
}
