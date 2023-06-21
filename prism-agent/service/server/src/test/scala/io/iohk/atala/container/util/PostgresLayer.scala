package io.iohk.atala.container.util

import cats.Functor
import cats.effect.std.Dispatcher
import cats.effect.{Async, Resource}
import cats.syntax.functor.*
import com.dimafeng.testcontainers.{JdbcDatabaseContainer, PostgreSQLContainer}
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import io.iohk.atala.shared.test.containers.PostgresTestContainer.postgresContainer
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.utility.DockerImageName
import zio.*
import zio.ZIO.*
import zio.interop.catz.*

import java.util.function.Consumer
import scala.concurrent.ExecutionContext

object PostgresLayer {

  def postgresLayer(
      imageName: Option[String] = Some("postgres"),
      verbose: Boolean = false
  ): TaskLayer[PostgreSQLContainer] =
    ZLayer.scoped {
      acquireRelease(ZIO.attemptBlockingIO {
        postgresContainer(imageName, verbose)
      })(container => attemptBlockingIO(container.stop()).orDie)
        // Start the container outside the aquireRelease as this might fail
        // to ensure contianer.stop() is added to the finalizer
        .tap(container => ZIO.attemptBlocking(container.start()))
    }

  private def hikariConfig(container: PostgreSQLContainer): HikariConfig = {
    val config = HikariConfig()
    config.setJdbcUrl(container.jdbcUrl)
    config.setUsername(container.username)
    config.setPassword(container.password)
    config
  }

  lazy val hikariConfigLayer: ZLayer[PostgreSQLContainer, Nothing, HikariConfig] =
    ZLayer.fromZIO {
      for {
        container <- ZIO.service[PostgreSQLContainer]
      } yield hikariConfig(container)
    }

  def transactor: ZLayer[HikariConfig, Throwable, Transactor[Task]] = ZLayer.fromZIO {
    val hikariTransactorLayerZIO = for {
      config <- ZIO.service[HikariConfig]
      htxResource: Resource[Task, HikariTransactor[Task]] = for {
        ec <- ExecutionContexts.cachedThreadPool[Task]
        xa <- HikariTransactor.fromHikariConfig[Task](config, ec)
      } yield xa
      layer <- Dispatcher.parallel[Task].allocated.map {
        case (dispatcher, _) => {
          given Dispatcher[Task] = dispatcher
          htxResource.toManaged.toLayer[Transactor[Task]]
        }
      }
    } yield layer
    hikariTransactorLayerZIO
  }.flatten

}
