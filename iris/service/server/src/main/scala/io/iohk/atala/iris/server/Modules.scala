package io.iohk.atala.iris.server

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.server.Route
import cats.effect.std.Dispatcher
import doobie.util.transactor.Transactor
import io.iohk.atala.iris.server.grpc.service.IrisServiceGrpcImpl
import io.iohk.atala.iris.server.grpc.{GrpcServer, GrpcServices}
import io.iohk.atala.iris.core.repository.OperationsRepository
import io.iohk.atala.iris.core.service.*
import io.iohk.atala.iris.core.worker.{MockPublishingScheduler, PublishingScheduler}
import io.iohk.atala.iris.proto.service.IrisServiceGrpc
import io.iohk.atala.iris.sql.repository.{JdbcOperationsRepository, TransactorLayer}
import zio.*
import zio.interop.catz.*

object Modules {
  val app: Task[Unit] = {
    val grpcServerApp = GrpcServices.services.flatMap(GrpcServer.start(8081, _))

    grpcServerApp
      .provideLayer(GrpcModule.layers)
      .unit
  }

}

// TODO: replace with actual implementation
object AppModule {
  val publishingServiceLayer: ULayer[PublishingService] = MockPublishingService.layer
  val publishingSchedulerLayer: ULayer[PublishingScheduler] = MockPublishingScheduler.layer
}

object GrpcModule {
  val irisServiceGrpcLayer: ULayer[IrisServiceGrpc.IrisService] = {
    val schedulerLayer = AppModule.publishingSchedulerLayer
    schedulerLayer >>> IrisServiceGrpcImpl.layer
  }

  val layers = irisServiceGrpcLayer
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
            jdbcUrl = "jdbc:postgresql://localhost:5432/iris"
          )
        )
      }
    }.flatten

  val operationsRepoLayer: TaskLayer[OperationsRepository[Task]] =
    transactorLayer >>> JdbcOperationsRepository.layer

  val layers = operationsRepoLayer
}
