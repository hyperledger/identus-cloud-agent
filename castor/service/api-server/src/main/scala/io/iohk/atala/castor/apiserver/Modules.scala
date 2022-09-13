package io.iohk.atala.castor.apiserver

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Route
import io.iohk.atala.castor.apiserver.grpc.service.DIDServiceGrpcImpl
import io.iohk.atala.castor.apiserver.grpc.{GrpcServer, GrpcServices}
import io.iohk.atala.castor.apiserver.http.{HttpRoutes, HttpServer}
import io.iohk.atala.castor.core.service.{
  DIDAuthenticationService,
  DIDOperationService,
  DIDService,
  MockDIDAuthenticationService,
  MockDIDOperationService,
  MockDIDService
}
import io.iohk.atala.castor.apiserver.http.marshaller.{
  DIDApiMarshallerImpl,
  DIDAuthenticationApiMarshallerImpl,
  DIDOperationsApiMarshallerImpl
}
import io.iohk.atala.castor.apiserver.http.service.{
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
import io.iohk.atala.castor.proto.castor_api.DIDServiceGrpc
import zio.*

object Modules {

  val actorSystemLayer: TaskLayer[ActorSystem[Nothing]] = ZLayer.scoped(
    ZIO.acquireRelease(ZIO.attempt(ActorSystem(Behaviors.empty, "actor-system")))(system =>
      ZIO.attempt(system.terminate()).orDie
    )
  )

  val app: Task[Unit] = {
    val httpServerApp = HttpRoutes.routes.flatMap(HttpServer.start(8080, _))
    val grpcServerApp = GrpcServices.services.flatMap(GrpcServer.start(8081, _))

    (httpServerApp <&> grpcServerApp)
      .provideLayer(actorSystemLayer ++ HttpModules.layers ++ GrpcModules.layers)
      .unit
  }

}

// TODO: replace with actual implementation
object AppModules {
  val didServiceLayer: ULayer[DIDService] = MockDIDService.layer
  val didAuthenticationServiceLayer: ULayer[DIDAuthenticationService] = MockDIDAuthenticationService.layer
  val didOperationServiceLayer: ULayer[DIDOperationService] = MockDIDOperationService.layer
}

object HttpModules {
  val didApiLayer: ULayer[DIDApi] = {
    val serviceLayer = AppModules.didServiceLayer
    val apiServiceLayer = serviceLayer >>> DIDApiServiceImpl.layer
    val apiMarshallerLayer = DIDApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDApi(_, _))
  }

  val didOperationsApiLayer: ULayer[DIDOperationsApi] = {
    val serviceLayer = AppModules.didOperationServiceLayer
    val apiServiceLayer = serviceLayer >>> DIDOperationsApiServiceImpl.layer
    val apiMarshallerLayer = DIDOperationsApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDOperationsApi(_, _))
  }

  val didAuthenticationApiLayer: ULayer[DIDAuthenticationApi] = {
    val serviceLayer = AppModules.didAuthenticationServiceLayer
    val apiServiceLayer = serviceLayer >>> DIDAuthenticationApiServiceImpl.layer
    val apiMarshallerLayer = DIDAuthenticationApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDAuthenticationApi(_, _))
  }

  val layers = didApiLayer ++ didOperationsApiLayer ++ didAuthenticationApiLayer
}

object GrpcModules {
  val didServiceGrpcLayer: ULayer[DIDServiceGrpc.DIDService] = {
    val serviceLayer = AppModules.didServiceLayer
    serviceLayer >>> DIDServiceGrpcImpl.layer
  }

  val layers = didServiceGrpcLayer
}
