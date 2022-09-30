package io.iohk.atala.pollux.server

import akka.actor.BootstrapSetup
import akka.actor.setup.ActorSystemSetup
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Route

import io.iohk.atala.pollux.server.http.{HttpRoutes, HttpServer}

import io.iohk.atala.pollux.server.http.marshaller.*
import io.iohk.atala.pollux.server.http.service.*
import io.iohk.atala.pollux.openapi.api.{
  IssueCredentialsApi,
  PresentProofApi,
  RevocationRegistryApi,
  SchemaRegistryApi
}

import zio.*

object Modules {

  val app: Task[Unit] = {
    val httpServerApp = HttpRoutes.routes.flatMap(HttpServer.start(8080, _))

    (httpServerApp)
      .provideLayer(SystemModule.actorSystemLayer ++ HttpModule.layers)
      .unit
  }

}

object SystemModule {
  val actorSystemLayer: TaskLayer[ActorSystem[Nothing]] = ZLayer.scoped(
    ZIO.acquireRelease(
      ZIO.executor
        .map(_.asExecutionContext)
        .flatMap(ec =>
          ZIO.attempt(ActorSystem(Behaviors.empty, "actor-system", BootstrapSetup().withDefaultExecutionContext(ec)))
        )
    )(system => ZIO.attempt(system.terminate()).orDie)
  )

}

object HttpModule {
  val issueCredentialsApiLayer: ULayer[IssueCredentialsApi] = {
    val apiServiceLayer = IssueCredentialsApiImpl.layer
    val apiMarshallerLayer = IssueCredentialsApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new IssueCredentialsApi(_, _))
  }

//  val PresentProofApi: ULayer[PresentProofApi] = {
//    val serviceLayer = AppModule.didOperationServiceLayer
//    val apiServiceLayer = serviceLayer >>> DIDOperationsApiServiceImpl.layer
//    val apiMarshallerLayer = DIDOperationsApiMarshallerImpl.layer
//    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new PresentProofApi(_, _))
//  }
//
//  val RevocationRegistryApi: ULayer[RevocationRegistryApi] = {
//    val serviceLayer = AppModule.didAuthenticationServiceLayer
//    val apiServiceLayer = serviceLayer >>> DIDAuthenticationApiServiceImpl.layer
//    val apiMarshallerLayer = DIDAuthenticationApiMarshallerImpl.layer
//    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new RevocationRegistryApi(_, _))
//  }
//
//  val SchemaRegistryApi: ULayer[SchemaRegistryApi] = {
//    val serviceLayer = AppModule.didAuthenticationServiceLayer
//    val apiServiceLayer = serviceLayer >>> DIDAuthenticationApiServiceImpl.layer
//    val apiMarshallerLayer = DIDAuthenticationApiMarshallerImpl.layer
//    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new SchemaRegistryApi(_, _))
//  }

//  val layers = ZLayer.suspend[Any, Throwable, IssueCredentialsApi](issueCredentialsApiLayer)
  val layers = issueCredentialsApiLayer
  // ++ PresentProofApi ++ RevocationRegistryApi ++ SchemaRegistryApi
}
