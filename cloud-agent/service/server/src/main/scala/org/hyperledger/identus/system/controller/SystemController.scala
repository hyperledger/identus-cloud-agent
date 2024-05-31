package org.hyperledger.identus.system.controller

import org.hyperledger.identus.api.http.ErrorResponse
import org.hyperledger.identus.api.http.RequestContext
import org.hyperledger.identus.system.controller.http.HealthInfo
import zio.*

trait SystemController {

  def health()(implicit rc: RequestContext): IO[ErrorResponse, HealthInfo]

  def metrics()(implicit rc: RequestContext): IO[ErrorResponse, String]

}
