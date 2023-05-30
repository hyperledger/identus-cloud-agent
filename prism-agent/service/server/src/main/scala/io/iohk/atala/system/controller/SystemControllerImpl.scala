package io.iohk.atala.system.controller

import io.iohk.atala.agent.server.buildinfo.BuildInfo
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.system.controller.http.HealthInfo
import zio.http.Response
import zio.metrics.connectors.prometheus
import zio.metrics.connectors.prometheus.PrometheusPublisher
import zio.{IO, URLayer, ZIO, ZLayer}

class SystemControllerImpl(
    prometheus: PrometheusPublisher
) extends SystemController {

  override def health()(implicit rc: RequestContext): IO[ErrorResponse, HealthInfo] = {
    ZIO.succeed(HealthInfo(version = BuildInfo.version))
  }

  override def metrics()(implicit rc: RequestContext): IO[ErrorResponse, String] = {
    prometheus.get
  }

}

object SystemControllerImpl {
  val layer: URLayer[PrometheusPublisher, SystemController] =
    ZLayer.fromFunction(SystemControllerImpl(_))
}
