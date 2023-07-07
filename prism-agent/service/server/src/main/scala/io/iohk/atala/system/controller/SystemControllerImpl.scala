package io.iohk.atala.system.controller

import io.iohk.atala.agent.server.buildinfo.BuildInfo
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.system.controller.http.HealthInfo
import io.micrometer.prometheus.PrometheusMeterRegistry
import zio.*

class SystemControllerImpl(
    prometheusRegistry: PrometheusMeterRegistry
) extends SystemController {

  override def health()(implicit rc: RequestContext): IO[ErrorResponse, HealthInfo] = {
    ZIO.succeed(HealthInfo(version = BuildInfo.version))
  }

  override def metrics()(implicit rc: RequestContext): IO[ErrorResponse, String] = {
    ZIO.succeed(prometheusRegistry.scrape)
  }

}

object SystemControllerImpl {
  val layer: URLayer[PrometheusMeterRegistry, SystemController] =
    ZLayer.fromFunction(SystemControllerImpl(_))
}
