package io.iohk.atala.castor.httpserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Route
import io.iohk.atala.castor.core.service.{
  DIDAuthenticationService,
  DIDService,
  MockDIDAuthenticationService,
  MockDIDOperationService,
  MockDIDService
}
import io.iohk.atala.castor.httpserver.api.marshaller.{
  DIDApiMarshallerImpl,
  DIDAuthenticationApiMarshallerImpl,
  DIDOperationsApiMarshallerImpl
}
import io.iohk.atala.castor.httpserver.api.service.{
  DIDApiServiceImpl,
  DIDAuthenticationApiServiceImpl,
  DIDOperationsApiServiceImpl
}
import io.iohk.atala.castor.openapi.api.{
  DIDApi,
  DIDAuthenticationApi,
  DIDAuthenticationApiMarshaller,
  DIDAuthenticationApiService,
  DIDOperationsApi
}
import zio.*

object Modules {

  val actorSystemLayer: TaskLayer[ActorSystem[Nothing]] = ZLayer.scoped(
    ZIO.acquireRelease(ZIO.attempt(ActorSystem(Behaviors.empty, "actor-system")))(system =>
      ZIO.attempt(system.terminate()).orDie
    )
  )

  val didApiLayer: ULayer[DIDApi] = {
    val serviceLayer = MockDIDService.layer // TODO: replace with actual implementation
    val apiServiceLayer = serviceLayer >>> DIDApiServiceImpl.layer
    val apiMarshallerLayer = DIDApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDApi(_, _))
  }

  val didOperationsApiLayer: ULayer[DIDOperationsApi] = {
    val serviceLayer = MockDIDOperationService.layer // TODO: replace with actual implementation
    val apiServiceLayer = serviceLayer >>> DIDOperationsApiServiceImpl.layer
    val apiMarshallerLayer = DIDOperationsApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDOperationsApi(_, _))
  }

  val didAuthenticationApiLayer: ULayer[DIDAuthenticationApi] = {
    val serviceLayer = MockDIDAuthenticationService.layer // TODO: replace with actual implementation
    val apiServiceLayer = serviceLayer >>> DIDAuthenticationApiServiceImpl.layer
    val apiMarshallerLayer = DIDAuthenticationApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDAuthenticationApi(_, _))
  }

  val app = {
    val serverApp = for {
      routes <- HttpRoutes.routes
      _ <- HttpServer.start(8000, routes)
    } yield ()

    serverApp.provideLayer(actorSystemLayer ++ didApiLayer ++ didOperationsApiLayer ++ didAuthenticationApiLayer)
  }

}
