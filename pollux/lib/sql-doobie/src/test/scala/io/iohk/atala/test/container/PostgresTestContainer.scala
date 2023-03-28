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
import org.testcontainers.containers.{JdbcDatabaseContainer => JavaJdbcDatabaseContainer}
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.utility.DockerImageName
import zio.*
import zio.ZIO.*
import zio.interop.catz.*

import java.util.function.Consumer
import scala.concurrent.ExecutionContext

class PostgreSQLContainerPlus(
  dockerImageNameOverride: Option[DockerImageName] = None,
  databaseName: Option[String] = None,
  pgUsername: Option[String] = None,
  pgPassword: Option[String] = None,
  mountPostgresDataToTmpfs: Boolean = false,
  urlParams: Map[String, String] = Map.empty,
  commonJdbcParams: JdbcDatabaseContainer.CommonParams = JdbcDatabaseContainer.CommonParams()
) extends PostgreSQLContainer(dockerImageNameOverride, databaseName, pgUsername, pgPassword, mountPostgresDataToTmpfs, urlParams, commonJdbcParams) with JdbcDatabaseContainerPlus {

  override def jdbcUrl: String = {
    // Custom implementation for the jdbcUrl method
    val origUrl = super.jdbcUrl
    val idx = origUrl.indexOf(',')
    val params = if (idx >= 0) origUrl.substring(idx) else ""
    println(origUrl)
    println(s"jdbc:postgresql://${containerId.take(12)}:5432/${databaseName}${params}")
    s"jdbc:postgresql://${containerId.take(12)}:5432/${databaseName}${params}"
  }
}

object PostgresTestContainer {

  def postgresLayer(
      imageName: Option[String] = Some("postgres"),
      verbose: Boolean = false
  ): ZLayer[Any, Nothing, PostgreSQLContainer] =
    ZLayer.scoped {
      acquireRelease(ZIO.attemptBlockingIO {
        val container = new PostgreSQLContainerPlus(
          dockerImageNameOverride = imageName.map(DockerImageName.parse)
        )

        sys.env.get("GITHUB_NETWORK").map { network => container.container.withNetworkMode(network) }

        if (verbose) {
          container.container
            .withLogConsumer(new Consumer[OutputFrame] {
              override def accept(t: OutputFrame): Unit = println(t.getUtf8String)
            })
          container.container
            .withCommand("postgres", "-c", "log_statement=all", "-c", "log_destination=stderr")
        }

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
