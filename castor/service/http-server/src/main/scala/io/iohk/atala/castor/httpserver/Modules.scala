package io.iohk.atala.castor.httpserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import zio.*

object Modules {

  val actorSystemLayer: TaskLayer[ActorSystem[Nothing]] = ZLayer.scoped(
    ZIO.acquireRelease(ZIO.attempt(ActorSystem(Behaviors.empty, "actor-system")))(system =>
      ZIO.attempt(system.terminate()).orDie
    )
  )

  val app = HttpServer.start(8000).provideLayer(actorSystemLayer)

}
