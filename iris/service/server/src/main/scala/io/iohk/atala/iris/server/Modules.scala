package io.iohk.atala.iris.server

import cats.effect.std.Dispatcher
import doobie.util.transactor.Transactor
import io.iohk.atala.iris.core.repository.*
import io.iohk.atala.iris.core.service.*
import io.iohk.atala.iris.core.worker.{MockPublishingScheduler, PublishingScheduler}
import io.iohk.atala.iris.proto.service.IrisServiceGrpc
import io.iohk.atala.iris.server.grpc.service.IrisServiceGrpcImpl
import io.iohk.atala.iris.server.grpc.{GrpcServer, GrpcServices}
import io.iohk.atala.iris.sql.repository
import io.iohk.atala.iris.sql.repository.*
import zio.*
import zio.interop.catz.*
import zio.stream.ZStream

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
  val irisServiceGrpcLayer: TaskLayer[IrisServiceGrpc.IrisService] = {
    val schedulerLayer = AppModule.publishingSchedulerLayer
    val irisBatchesLayer = RepoModule.irisBatchesRepoLayer
    (schedulerLayer ++ irisBatchesLayer) >>> IrisServiceGrpcImpl.layer
  }

  val layers = irisServiceGrpcLayer
}

object BlockchainModule {
  def blocksStreamerLayer(config: BlocksStreamer.Config): TaskLayer[BlocksStreamer] = {
    val blocksRepoLayer = RepoModule.blocksRepoLayer
    val keyValueRepoLayer = RepoModule.keyValueRepoLayer
    (blocksRepoLayer ++ keyValueRepoLayer) >>> BlocksStreamer.layer(config)
  }

  val blocksSaverLayer: TaskLayer[BlocksSaveSinker] = {
    val keyValueIO = JdbcKeyValueRepositoryIO.layer
    val irisBatchesIO = JdbcIrisBatchRepositoryIO.layer
    val dbRepositoryTransactorIO = RepoModule.dbRepositoryTransactor
    (keyValueIO ++ irisBatchesIO ++ dbRepositoryTransactorIO) >>> BlocksSaveSinker
      .layer[repository.IO, repository.StreamIO]
  }
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

  val dbRepositoryTransactor: TaskLayer[JdbcDbRepositoryTransactorIO] =
    transactorLayer >>> JdbcDbRepositoryTransactorIO.layer

  val operationsRepoLayer: TaskLayer[OperationsRepository[Task]] =
    transactorLayer >>> JdbcOperationsRepository.layer

  val irisBatchesRepoLayer: TaskLayer[IrisBatchesRepository[Task, StreamZIO]] =
    (transactorLayer ++ JdbcIrisBatchRepositoryIO.layer) >>> JdbcIrisBatchRepository.layer

  val blocksRepoLayer: TaskLayer[ROBlocksRepository[Task]] =
    transactorLayer >>> JdbcBlocksRepository.layer

  val keyValueRepoLayer: TaskLayer[KeyValueRepository[Task]] =
    (transactorLayer ++ JdbcKeyValueRepositoryIO.layer) >>> JdbcKeyValueRepository.layer

  val layers = operationsRepoLayer ++ irisBatchesRepoLayer ++ blocksRepoLayer ++ keyValueRepoLayer
}
