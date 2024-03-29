package io.iohk.atala.verification.controller

import io.iohk.atala.agent.server.buildinfo.BuildInfo
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.system.controller.http.HealthInfo
import io.iohk.atala.verification.controller
import io.micrometer.prometheus.PrometheusMeterRegistry
import zio.*

class VcVerificationControllerImpl extends VcVerificationController {

  override def verify(
      request: controller.http.VcVerificationRequests
  )(implicit rc: RequestContext): IO[ErrorResponse, controller.http.VcVerificationResponses] = {
    ZIO.succeed(controller.http.VcVerificationResponses(credentialVerificationResponses = List.empty))
  }
}

object VcVerificationControllerImpl {
  val layer: ULayer[VcVerificationController] =
    ZLayer.succeed(VcVerificationControllerImpl())
}
