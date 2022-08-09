package io.iohk.atala

import cats.syntax.all.*
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio.interop.catz.*
import zio.{Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault}

import scala.io.StdIn

object Main extends ZIOAppDefault {

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val routes =
      ZHttp4sServerInterpreter().from(Endpoints.all).toRoutes <+> new SwaggerHttp4s(Endpoints.yaml).routes

    BlazeServerBuilder[Task]
      .bindHttp(8080, "localhost")
      .withHttpApp(Router("/" -> routes).orNotFound)
      .resource
      .use { _ =>
        ZIO.succeedBlocking {
          println(
            "Server started at http://localhost:8080. \n Open API docs at http://localhost:8080/docs. \n Press ENTER key to exit."
          )
          StdIn.readLine()
        }
      }

  }
}
