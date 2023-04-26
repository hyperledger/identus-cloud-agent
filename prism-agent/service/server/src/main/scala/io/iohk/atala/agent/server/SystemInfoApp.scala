package io.iohk.atala.agent.server

import zio.*
import zio.http.*
import zio.http.model.*
import zio.metrics.jvm.DefaultJvmMetrics
import io.iohk.atala.agent.server.buildinfo.BuildInfo
import io.iohk.atala.agent.server.health.HealthInfo
import zio.metrics.connectors.MetricsConfig
import zio.metrics.connectors.prometheus

import io.circe.generic.auto.*
import io.circe.syntax.*

object SystemInfoApp {
  val metricsConfig = ZLayer.succeed(MetricsConfig(5.seconds))

  lazy val prometheusLayer = (metricsConfig ++ prometheus.publisherLayer) >>> prometheus.prometheusLayer

  def app = Http
    .collectZIO[Request] {
      case Method.GET -> !! / "metrics" =>
        ZIO.serviceWithZIO[prometheus.PrometheusPublisher](_.get.map(Response.text))
      case Method.GET -> !! / "health" =>
        ZIO.succeed(Response.json(HealthInfo(version = BuildInfo.version).asJson.toString))
    }
    .provideLayer(SystemInfoApp.metricsConfig ++ prometheus.publisherLayer ++ prometheusLayer)
}
