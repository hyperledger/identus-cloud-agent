package io.iohk.atala.castor.httpserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import io.iohk.atala.castor.openapi.api.DIDAuthenticationApi
import zio.*

object Layers {
  val app = HttpServer.start()(8000)
}
