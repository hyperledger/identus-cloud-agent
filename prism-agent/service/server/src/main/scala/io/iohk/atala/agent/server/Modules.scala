package io.iohk.atala.agent.server

import akka.actor.BootstrapSetup
import akka.actor.setup.ActorSystemSetup
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Route
import doobie.util.transactor.Transactor
import io.iohk.atala.agent.server.http.{HttpRoutes, HttpServer}
import io.iohk.atala.agent.core.service.{
  DIDAuthenticationService,
  DIDOperationService,
  DIDService,
  MockDIDAuthenticationService,
  MockDIDOperationService,
  MockDIDService,
  MockIrisNotificationService
}
import io.iohk.atala.agent.server.http.marshaller.{
  DIDApiMarshallerImpl,
  DIDAuthenticationApiMarshallerImpl,
  DIDOperationsApiMarshallerImpl
}
import io.iohk.atala.agent.server.http.service.{
  DIDApiServiceImpl,
  DIDAuthenticationApiServiceImpl,
  DIDOperationsApiServiceImpl
}
import io.iohk.atala.agent.core.repository.DIDOperationRepository
import io.iohk.atala.agent.openapi.api.{
  DIDApi,
  DIDAuthenticationApi,
  DIDAuthenticationApiMarshaller,
  DIDAuthenticationApiService,
  DIDOperationsApi
}
import io.iohk.atala.agent.sql.repository.{JdbcDIDOperationRepository, TransactorLayer}
import zio.*
import zio.interop.catz.*
import cats.effect.std.Dispatcher
import io.iohk.atala.agent.server.worker.{EventConsumer, WorkerApp}
import io.iohk.atala.agent.core.model.IrisNotification
import zio.stream.ZStream

import java.util.concurrent.Executors

object Modules {

  val app: Task[Unit] = {
    val httpServerApp = HttpRoutes.routes.flatMap(HttpServer.start(8080, _))
    val workerApp = WorkerApp.start.provideSomeLayer(SystemModule.workerRuntimeLayer)

    (httpServerApp <&> workerApp)
      .provideLayer(SystemModule.actorSystemLayer ++ HttpModule.layers ++ WorkerModule.layers)
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

  val workerRuntimeLayer: ULayer[Unit] = Runtime.setExecutor(
    Executor.fromJavaExecutor(Executors.newFixedThreadPool(8, new Thread(_, "castor-worker")))
  )
}

// TODO: replace with actual implementation
object AppModule {
  val didServiceLayer: ULayer[DIDService] = MockDIDService.layer
  val didAuthenticationServiceLayer: ULayer[DIDAuthenticationService] = MockDIDAuthenticationService.layer
  val didOperationServiceLayer: ULayer[DIDOperationService] = MockDIDOperationService.layer
}

object HttpModule {
  val didApiLayer: ULayer[DIDApi] = {
    val serviceLayer = AppModule.didServiceLayer
    val apiServiceLayer = serviceLayer >>> DIDApiServiceImpl.layer
    val apiMarshallerLayer = DIDApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDApi(_, _))
  }

  val didOperationsApiLayer: ULayer[DIDOperationsApi] = {
    val serviceLayer = AppModule.didOperationServiceLayer
    val apiServiceLayer = serviceLayer >>> DIDOperationsApiServiceImpl.layer
    val apiMarshallerLayer = DIDOperationsApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDOperationsApi(_, _))
  }

  val didAuthenticationApiLayer: ULayer[DIDAuthenticationApi] = {
    val serviceLayer = AppModule.didAuthenticationServiceLayer
    val apiServiceLayer = serviceLayer >>> DIDAuthenticationApiServiceImpl.layer
    val apiMarshallerLayer = DIDAuthenticationApiMarshallerImpl.layer
    (apiServiceLayer ++ apiMarshallerLayer) >>> ZLayer.fromFunction(new DIDAuthenticationApi(_, _))
  }

  val layers = didApiLayer ++ didOperationsApiLayer ++ didAuthenticationApiLayer
}

object WorkerModule {
  // TODO: replace with actual implementation
  val irisNotificationSource: ULayer[ZStream[Any, Nothing, IrisNotification]] = ZLayer.succeed {
    ZStream
      .tick(1.seconds)
      .as(IrisNotification(foo = "bar"))
  }

  val eventConsumerLayer: ULayer[EventConsumer] = {
    val serviceLayer = MockIrisNotificationService.layer // TODO: replace with actual implementation
    serviceLayer >>> EventConsumer.layer
  }

  val layers = irisNotificationSource ++ eventConsumerLayer
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
