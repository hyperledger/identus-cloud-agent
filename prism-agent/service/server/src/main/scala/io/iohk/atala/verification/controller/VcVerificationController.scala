package io.iohk.atala.verification.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.verification.controller
import zio.*

trait VcVerificationController {

  def verify(request: List[controller.http.VcVerificationRequest])(implicit
      rc: RequestContext
  ): IO[ErrorResponse, List[controller.http.VcVerificationResponse]]
}
