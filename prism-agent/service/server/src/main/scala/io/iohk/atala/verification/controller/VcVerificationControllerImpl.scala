package io.iohk.atala.verification.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.verification.controller
import zio.*

class VcVerificationControllerImpl extends VcVerificationController {

  override def verify(
      request: List[controller.http.VcVerificationRequest]
  )(implicit rc: RequestContext): IO[ErrorResponse, List[controller.http.VcVerificationResponse]] = {
    ZIO.succeed(List.empty)
  }
}

object VcVerificationControllerImpl {
  val layer: ULayer[VcVerificationController] =
    ZLayer.succeed(VcVerificationControllerImpl())
}
