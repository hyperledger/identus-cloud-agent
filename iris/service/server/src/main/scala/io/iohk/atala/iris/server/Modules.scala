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
import com.typesafe.config.ConfigFactory
import io.iohk.atala.iris.server.config.AppConfig
import zio.config.typesafe.TypesafeConfigSource
import zio.config.{ReadError, read}

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

  val configLayer: Layer[ReadError[String], AppConfig] = ZLayer.fromZIO {
    read(
      AppConfig.descriptor.from(
        TypesafeConfigSource.fromTypesafeConfig(
          ZIO.attempt(ConfigFactory.load())
        )
      )
    )
  }
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
  val transactorLayer: TaskLayer[Transactor[Task]] = {
    val layerWithConfig = ZLayer.fromZIO {
      ZIO.service[AppConfig].map(_.iris.database).flatMap { config =>
        Dispatcher[Task].allocated.map { case (dispatcher, _) =>
          given Dispatcher[Task] = dispatcher
          TransactorLayer.hikari[Task](
            TransactorLayer.DbConfig(
              username = config.username,
              password = config.password,
              jdbcUrl = s"jdbc:postgresql://${config.host}:${config.port}/${config.databaseName}"
            )
          )
        }
      }
    }.flatten
    AppModule.configLayer >>> layerWithConfig
  }

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
