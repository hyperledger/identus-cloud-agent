package io.iohk.atala.agent.server

import zio.{ZLayer, *}
import zio.http.*
import zio.http.model.*
import zio.metrics.jvm.DefaultJvmMetrics
import io.iohk.atala.agent.server.buildinfo.BuildInfo
import io.iohk.atala.agent.server.health.HealthInfo
import zio.metrics.connectors.{MetricsConfig, prometheus}
import io.circe.generic.auto.*
import io.circe.syntax.*

object SystemInfoApp {
  def app = Http
    .collectZIO[Request] {
      case Method.GET -> !! / "metrics" =>
        ZIO.serviceWithZIO[prometheus.PrometheusPublisher](_.get.map(Response.text))
      case Method.GET -> !! / "health" =>
        ZIO.succeed(Response.json(HealthInfo(version = BuildInfo.version).asJson.toString))
    }
}
