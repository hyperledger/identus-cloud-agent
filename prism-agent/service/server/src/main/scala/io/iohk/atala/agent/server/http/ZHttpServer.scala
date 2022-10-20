package io.iohk.atala.agent.server.http

import org.slf4j.LoggerFactory
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zhttp.http.HttpApp
import zhttp.service.server.ServerChannelFactory
import zhttp.service.{EventLoopGroup, Server}
import zio.{Console, RIO, Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}
import sttp.tapir.ztapir.ZServerEndpoint
import io.iohk.atala.agent.server.http.ZHttpEndpoints
import io.iohk.atala.pollux.service.{SchemaRegistryService, SchemaRegistryServiceInMemory}
import io.iohk.atala.pollux.schema.SchemaRegistryServerEndpoints

object ZHttpServer {
  val log = LoggerFactory.getLogger(ZioHttpInterpreter.getClass.getName)

  val defaultPort = sys.env.get("http.port").map(_.toInt).getOrElse(8085)

  val defaultServerOptions: ZioHttpServerOptions[Any] =
    ZioHttpServerOptions.customiseInterceptors
      .serverLog(
        DefaultServerLog[Task](
          doLogWhenReceived = msg => ZIO.succeed(log.debug(msg)),
          doLogWhenHandled = (msg, error) => ZIO.succeed(error.fold(log.debug(msg))(err => log.debug(msg, err))),
          doLogAllDecodeFailures = (msg, error) => ZIO.succeed(error.fold(log.debug(msg))(err => log.debug(msg, err))),
          doLogExceptions = (msg: String, ex: Throwable) => ZIO.succeed(log.debug(msg, ex)),
          noLog = ZIO.unit
        )
      )
      .options

  def start(
      endpoints: List[ZServerEndpoint[Any, Any]],
      port: Int = defaultPort,
      serverOptions: ZioHttpServerOptions[Any] = defaultServerOptions
  ): ZIO[Scope, Throwable, Any] =
    val app: HttpApp[Any, Throwable] = ZioHttpInterpreter(serverOptions).toHttp(endpoints)

    (for {
      _ <- Server(app).withPort(port).make
      _ <- Console.printLine(s"Go to http://localhost:$port/docs to open SwaggerUI")
      _ <- ZIO.never
    } yield ())
      .provideSomeLayer(EventLoopGroup.auto(0) ++ ServerChannelFactory.auto ++ Scope.default)
}
