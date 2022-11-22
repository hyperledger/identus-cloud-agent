package io.iohk.atala.agent.server.http

import cats.implicits.*
import io.iohk.atala.agent.server.http.ZHttpEndpoints
import io.iohk.atala.pollux.schema.SchemaRegistryServerEndpoints
import io.iohk.atala.pollux.service.{SchemaRegistryService, SchemaRegistryServiceInMemory}
import org.http4s.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.slf4j.LoggerFactory
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.ztapir.ZServerEndpoint
import zhttp.http.HttpApp
import zhttp.service.server.ServerChannelFactory
import zhttp.service.{EventLoopGroup, Server}
import zio.interop.catz.*
import zio.*

import scala.concurrent.ExecutionContext.Implicits.global

object ZHttp4sBlazeServer {
  def start(
      endpoints: List[ZServerEndpoint[Any, Any]],
      port: Int
  ): Task[ExitCode] = {
    val http4sEndpoints: HttpRoutes[Task] =
      ZHttp4sServerInterpreter().from(endpoints).toRoutes

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
  }
}
