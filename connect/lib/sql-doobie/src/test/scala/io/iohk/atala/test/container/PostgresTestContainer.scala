package io.iohk.atala.test.container

import cats.Functor
import cats.effect.std.Dispatcher
import cats.effect.{Async, Resource}
import cats.syntax.functor.*
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.utility.DockerImageName
import zio.*
import zio.ZIO.*
import zio.interop.catz.*
import io.iohk.atala.shared.test.containers.PostgresTestContainer.postgresContainer

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

}
