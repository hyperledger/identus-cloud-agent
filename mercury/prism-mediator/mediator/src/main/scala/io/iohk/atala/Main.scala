package io.iohk.atala

import cats.syntax.all._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio.interop.catz._
import zio.{Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault}
import org.http4s._

import scala.io.StdIn

import cats.syntax.all._
import io.circe.generic.auto._
import org.http4s._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import sttp.tapir.PublicEndpoint
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.ztapir._
import zio.interop.catz._
import zio.{ExitCode, Task, URIO, ZIO, ZIOAppDefault}

import zio._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.Endpoint

type MyTask[+A] = // [_] =>> zio.RIO[io.iohk.atala.DIDCommService, _]
  ZIO[DIDCommService, Throwable, A] // TODO improve this Throwable (is too much)

object Main extends ZIOAppDefault {

  // API
  val apiRoutes: org.http4s.HttpRoutes[MyTask] = ZHttp4sServerInterpreter()
    .from(Endpoints.all)
    .toRoutes

  // Documentation
  val swaggerRoutes: HttpRoutes[MyTask] = {
    val allEndpoints: List[Endpoint[_, _, _, _, _]] = Endpoints.all.map(_.endpoint)
    ZHttp4sServerInterpreter()
      .from(SwaggerInterpreter().fromEndpoints[MyTask](allEndpoints, "Atala Prism Mediator", "0.1.0"))
      .toRoutes
  }

  //
  val serve: ZIO[DIDCommService, Throwable, Unit] = {
    Console.printLine("""
        |   ███╗   ███╗███████╗██████╗  ██████╗██╗   ██╗██████╗ ██╗   ██╗
        |   ████╗ ████║██╔════╝██╔══██╗██╔════╝██║   ██║██╔══██╗╚██╗ ██╔╝
        |   ██╔████╔██║█████╗  ██████╔╝██║     ██║   ██║██████╔╝ ╚████╔╝
        |   ██║╚██╔╝██║██╔══╝  ██╔══██╗██║     ██║   ██║██╔══██╗  ╚██╔╝
        |   ██║ ╚═╝ ██║███████╗██║  ██║╚██████╗╚██████╔╝██║  ██║   ██║
        |   ╚═╝     ╚═╝╚══════╝╚═╝  ╚═╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝
        |""".stripMargin) *>
      Console.printLine(
        """#####################################################
          |###  Starting the server at http://localhost:8080 ###
          |###  Open API docs at http://localhost:8080/docs  ###
          |###  Press ENTER key to exit.                     ###
          |#####################################################""".stripMargin // FIXME But server is not shutting down
      ) *>
      ZIO.executor.flatMap(executor =>
        BlazeServerBuilder[MyTask]
          .withExecutionContext(executor.asExecutionContext)
          .bindHttp(8080, "localhost")
          .withHttpApp(Router("/" -> (apiRoutes <+> swaggerRoutes)).orNotFound)
          .serve
          .compile
          .drain
      )
  }

  override def run =
    serve.provide(AgentService.mediator: ZLayer[Any, Nothing, DIDCommService]).exitCode
}
