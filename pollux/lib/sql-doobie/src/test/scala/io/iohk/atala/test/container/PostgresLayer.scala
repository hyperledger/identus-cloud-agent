package io.iohk.atala.test.container

import cats.Functor
import cats.effect.std.Dispatcher
import cats.effect.{Async, Resource}
import cats.syntax.functor.*
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.JdbcDatabaseContainer
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
  ): ZLayer[Any, Nothing, PostgreSQLContainer] =
    ZLayer.scoped {
      acquireRelease(ZIO.attemptBlockingIO {
        val container = postgresContainer(imageName, verbose)
        container.start()
        container
      }.orDie)(container => attemptBlockingIO(container.stop()).orDie)
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
      layer <- Dispatcher[Task].allocated.map {
        case (dispatcher, _) => {
          given Dispatcher[Task] = dispatcher
          htxResource.toManaged.toLayer[Transactor[Task]]
        }
      }
    } yield layer
    hikariTransactorLayerZIO
  }.flatten

}
