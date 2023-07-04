package io.iohk.atala.system.controller

import zio.*
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.system.controller.http.HealthInfo

trait SystemController {

  def health()(implicit rc: RequestContext): IO[ErrorResponse, HealthInfo]

  def metrics()(implicit rc: RequestContext): IO[ErrorResponse, String]

}
