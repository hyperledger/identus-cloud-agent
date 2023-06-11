package io.iohk.atala.pollux

import io.iohk.atala.pollux.stub.SchemaEndpointsInMemory
import org.slf4j.LoggerFactory
import sttp.tapir.server.interceptor.log.DefaultServerLog
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zhttp.http.HttpApp
import zhttp.service.server.ServerChannelFactory
import zhttp.service.{EventLoopGroup, Server}
import zio.{Console, Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault}

// Here serverEndpoints are interpreted by Tapir into HTTP Server based on ZIO-HTTP Interpreter
object Main extends ZIOAppDefault:
  val log = LoggerFactory.getLogger(ZioHttpInterpreter.getClass.getName)

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    val serverOptions: ZioHttpServerOptions[Any] =
      ZioHttpServerOptions.customiseInterceptors
        .serverLog(
          DefaultServerLog[Task](
            doLogWhenReceived = msg => ZIO.succeed(log.debug(msg)),
            doLogWhenHandled = (msg, error) => ZIO.succeed(error.fold(log.debug(msg))(err => log.debug(msg, err))),
            doLogAllDecodeFailures =
              (msg, error) => ZIO.succeed(error.fold(log.debug(msg))(err => log.debug(msg, err))),
            doLogExceptions = (msg: String, ex: Throwable) => ZIO.succeed(log.debug(msg, ex)),
            noLog = ZIO.unit
          )
        )
        .options
    val app: HttpApp[Any, Throwable] = ZioHttpInterpreter(serverOptions)
      .toHttp(SchemaEndpointsInMemory.all)

    val port = sys.env.get("http.port").map(_.toInt).getOrElse(8080)

    (for
      serverStart <- Server(app).withPort(port).make
      _ <- Console.printLine(
        s"Go to http://localhost:${serverStart.port}/docs to open SwaggerUI. Press ENTER key to exit."
      )
      _ <- Console.readLine
    yield serverStart)
      .provideSomeLayer(EventLoopGroup.auto(0) ++ ServerChannelFactory.auto ++ Scope.default)
      .exitCode
