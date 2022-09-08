package io.iohk.atala.castor.httpserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.Http
import zio.*

object HttpServer {
  def start(port: Int): ZIO[ActorSystem[Nothing], Throwable, Unit] = {
    val routes = get { complete("Hello") } // TODO: replace with actual implementation

    val managedBinding =
      ZIO
        .acquireRelease(
          for {
            system <- ZIO.service[ActorSystem[Nothing]]
            _ <- ZIO.logInfo(s"starting http server on port $port")
            binding <- ZIO.fromFuture { _ =>
              given ActorSystem[Nothing] = system
              Http().newServerAt("0.0.0.0", port).bind(routes)
            }
            _ <- ZIO.logInfo(s"http server listening on port $port")
          } yield binding
        )(binding =>
          import scala.concurrent.duration._
          for {
            _ <- ZIO.logInfo("stopping http server")
            _ <- ZIO.fromFuture(_ => binding.terminate(10.seconds)).orDie
            _ <- ZIO.logInfo("http server stopped successfully")
          } yield ()
        )

    ZIO
      .scoped {
        for {
          _ <- managedBinding
          _ <- ZIO.never
        } yield ()
      }
  }
}
