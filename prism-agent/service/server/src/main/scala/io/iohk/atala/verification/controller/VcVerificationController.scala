package io.iohk.atala.verification.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.pollux.core.service.verification.VcVerificationServiceError
import io.iohk.atala.verification.controller
import zio.*

trait VcVerificationController {

  def verify(request: List[controller.http.VcVerificationRequest])(implicit
      rc: RequestContext
  ): IO[ErrorResponse, List[controller.http.VcVerificationResponse]]
}

object VcVerificationController {
  def toHttpError(error: VcVerificationServiceError): ErrorResponse =
    error match
      case VcVerificationServiceError.UnexpectedError(error) =>
        ErrorResponse.badRequest(detail = Some(s"VC Verification Failed: $error"))

}
