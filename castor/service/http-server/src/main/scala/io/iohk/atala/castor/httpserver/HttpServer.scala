package io.iohk.atala.castor.httpserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.Http
import io.iohk.atala.castor.openapi.api.DIDAuthenticationApi
import zio.*

object HttpServer {
  def start()(port: Int): Task[Unit] = {
    val routes = get { complete("Hello") } // TODO: replace with actual implementation

    val managedActorSystem: ZIO[Scope, Throwable, ActorSystem[Nothing]] =
      ZIO.acquireRelease(
        for {
          _ <- ZIO.logInfo("starting actor system")
          system <- ZIO.attempt(ActorSystem(Behaviors.empty, "http-actor-system"))
        } yield system
      )(sys =>
        for {
          _ <- ZIO.logInfo("stopping actor system")
          _ <- ZIO.attempt(sys.terminate()).orDie
        } yield ()
      )

    val managedBinding = (system: ActorSystem[Nothing]) =>
      ZIO
        .acquireRelease(
          for {
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
          system <- managedActorSystem
          _ <- managedBinding(system)
          _ <- ZIO.unit.delay(10.seconds) // TODO: remove
        } yield ()
      }
  }
}
