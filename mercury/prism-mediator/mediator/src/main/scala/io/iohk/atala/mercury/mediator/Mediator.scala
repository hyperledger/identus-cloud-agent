package io.iohk.atala.mercury.mediator

import cats.syntax.all._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio.interop.catz._
import org.http4s._

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

import io.iohk.atala.mercury.DidComm
import io.iohk.atala.mercury.AgentService

type MyTask[+A] = // [_] =>> zio.RIO[io.iohk.atala.DidComm, _]
  ZIO[DidComm & MailStorage, Throwable, A] // TODO improve this Throwable (is too much)

object Mediator extends ZIOAppDefault {

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

  val serve: ZIO[DidComm & MailStorage, Throwable, Unit] = MediatorProgram.startLogo *>
    ZIO.executor.flatMap(executor =>
      BlazeServerBuilder[MyTask]
        .withExecutionContext(executor.asExecutionContext)
        .bindHttp(MediatorProgram.port, "localhost")
        .withHttpApp(Router("/" -> (apiRoutes <+> swaggerRoutes)).orNotFound)
        .serve
        .compile
        .drain
    )

  override def run =
    serve
      .provide(
        (AgentService.mediator: ZLayer[Any, Nothing, DidComm]) ++
          MailStorage.layer
      )
      .exitCode
}
