package io.iohk.atala.agent.server

import akka.actor.BootstrapSetup
import akka.actor.setup.ActorSystemSetup
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Route
import doobie.util.transactor.Transactor
import io.iohk.atala.agent.server.http.{HttpRoutes, HttpServer}
import io.iohk.atala.castor.core.service.{DIDService, DIDServiceImpl}
import io.iohk.atala.agent.server.http.marshaller.{
  DIDApiMarshallerImpl,
  DIDAuthenticationApiMarshallerImpl,
  DIDOperationsApiMarshallerImpl,
  IssueCredentialsApiMarshallerImpl
}
import io.iohk.atala.agent.server.http.service.{
  DIDApiServiceImpl,
  DIDAuthenticationApiServiceImpl,
  DIDOperationsApiServiceImpl,
  IssueCredentialsApiServiceImpl
}
import io.iohk.atala.castor.core.repository.DIDOperationRepository
import io.iohk.atala.agent.openapi.api.{DIDApi, DIDAuthenticationApi, DIDOperationsApi, IssueCredentialsApi}
import io.iohk.atala.castor.sql.repository.{JdbcDIDOperationRepository, TransactorLayer}
import zio.*
import zio.interop.catz.*
import cats.effect.std.Dispatcher
import io.grpc.ManagedChannelBuilder
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.iris.proto.service.IrisServiceGrpc
import io.iohk.atala.iris.proto.service.IrisServiceGrpc.IrisServiceStub
import zio.stream.ZStream

import java.util.concurrent.Executors

object Modules {

  val app: Task[Unit] = {
    val httpServerApp = HttpRoutes.routes.flatMap(HttpServer.start(8080, _))

    httpServerApp
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

object AppModule {
  val didOpValidatorLayer: ULayer[DIDOperationValidator] = DIDOperationValidator.layer(
    DIDOperationValidator.Config(
      publicKeyLimit = 50,
      serviceLimit = 50
    )
  )
  val didServiceLayer: TaskLayer[DIDService] =
    (GrpcModule.irisStub ++ RepoModule.layers ++ didOpValidatorLayer) >>> DIDServiceImpl.layer
}

object GrpcModule {
  val irisStub: TaskLayer[IrisServiceStub] = ZLayer.fromZIO(
    ZIO.attempt {
      val channel = ManagedChannelBuilder.forAddress("localhost", 8081).usePlaintext.build
      IrisServiceGrpc.stub(channel)
    }
  )
}

object HttpModule {
  val didApiLayer: TaskLayer[DIDApi] = {
    val serviceLayer = AppModule.didServiceLayer
    val apiServiceLayer = serviceLayer >>> DIDApiServiceImpl.layer
    val apiMarshallerLayer = DIDApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDApi(_, _))
  }

  val didOperationsApiLayer: ULayer[DIDOperationsApi] = {
    val apiServiceLayer = DIDOperationsApiServiceImpl.layer
    val apiMarshallerLayer = DIDOperationsApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDOperationsApi(_, _))
  }

  val didAuthenticationApiLayer: ULayer[DIDAuthenticationApi] = {
    val apiServiceLayer = DIDAuthenticationApiServiceImpl.layer
    val apiMarshallerLayer = DIDAuthenticationApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDAuthenticationApi(_, _))
  }

  val issueCredentialsApiLayer: ULayer[IssueCredentialsApi] = {
    val apiServiceLayer = IssueCredentialsApiServiceImpl.layer
    val apiMarshallerLayer = IssueCredentialsApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new IssueCredentialsApi(_, _))
  }

  val layers = didApiLayer ++ didOperationsApiLayer ++ didAuthenticationApiLayer ++ issueCredentialsApiLayer
}

object RepoModule {
  val transactorLayer: TaskLayer[Transactor[Task]] =
    ZLayer.fromZIO {
      Dispatcher[Task].allocated.map { case (dispatcher, _) =>
        given Dispatcher[Task] = dispatcher
        TransactorLayer.hikari[Task](
          TransactorLayer.DbConfig(
            username = "postgres",
            password = "postgres",
            jdbcUrl = "jdbc:postgresql://localhost:5432/castor"
          )
        )
      }
    }.flatten

  val didOperationRepoLayer: TaskLayer[DIDOperationRepository[Task]] =
    transactorLayer >>> JdbcDIDOperationRepository.layer

  val layers = didOperationRepoLayer
}
