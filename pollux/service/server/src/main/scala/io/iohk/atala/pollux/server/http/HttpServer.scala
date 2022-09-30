package io.iohk.atala.pollux.server.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import zio.*

object HttpServer {

  def start(port: Int, routes: Route): RIO[ActorSystem[Nothing], Unit] = {
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
          import scala.concurrent.duration.*
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
