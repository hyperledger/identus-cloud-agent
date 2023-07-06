package io.iohk.atala.agent.server.http

import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.system.controller.SystemEndpoints
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.http4s.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import sttp.tapir.*
import sttp.tapir.server.http4s.Http4sServerOptions
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.ztapir.ZServerEndpoint
import zio.*
import zio.interop.catz.*

class ZHttp4sBlazeServer(micrometerRegistry: PrometheusMeterRegistry) {

  private val tapirPrometheusMetrics = PrometheusMetrics.default[Task](registry = micrometerRegistry.getPrometheusRegistry)

  private val serverOptions: Http4sServerOptions[Task] = Http4sServerOptions
    .customiseInterceptors[Task]
    .defaultHandlers(ErrorResponse.failureResponseHandler)
    .metricsInterceptor(tapirPrometheusMetrics.metricsInterceptor(
      ignoreEndpoints = Seq(SystemEndpoints.metrics)
    ))
    .options

  def start(
      endpoints: List[ZServerEndpoint[Any, Any]],
      port: Int
  ): Task[ExitCode] = {

    ZIO.attempt {
      val http4sEndpoints: HttpRoutes[Task] =
        ZHttp4sServerInterpreter(serverOptions)
          .from(endpoints)
          .toRoutes

      val serve: Task[Unit] =
        ZIO.executor.flatMap(executor =>
          BlazeServerBuilder[Task]
            .withExecutionContext(executor.asExecutionContext)
            .bindHttp(port, "0.0.0.0")
            .withHttpApp(Router("/" -> http4sEndpoints).orNotFound)
            .serve
            .compile
            .drain
        )

      serve.exitCode
    }.flatten

  }
}

object ZHttp4sBlazeServer {
  def make: URIO[PrometheusMeterRegistry, ZHttp4sBlazeServer] = {
    for {
      micrometerRegistry <- ZIO.service[PrometheusMeterRegistry]
      zHttp4sBlazeServer = ZHttp4sBlazeServer(micrometerRegistry)
    } yield zHttp4sBlazeServer
  }
}
