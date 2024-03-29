package io.iohk.atala.verification.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.verification.controller
import zio.*

trait VcVerificationController {

  def verify(request: controller.http.VcVerificationRequests)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, controller.http.VcVerificationResponses]
}
