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
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.utility.DockerImageName
import zio.*
import zio.ZIO.*
import zio.interop.catz.*

import java.util.function.Consumer
import scala.concurrent.ExecutionContext

object PostgresTestContainer {

  def postgresLayer(
      imageName: Option[String] = Some("postgres"),
      verbose: Boolean = false
  ): ZLayer[Any, Nothing, PostgreSQLContainer] =
    ZLayer.scoped {
      acquireRelease(ZIO.attemptBlockingIO {
        val container = new PostgreSQLContainer(
          dockerImageNameOverride = imageName.map(DockerImageName.parse)
        )

        sys.env.get("GITHUB_NETWORK").foreach { network =>
          container.container.withNetworkMode(network)
        }

        if (verbose) {
          container.container
            .withLogConsumer(new Consumer[OutputFrame] {
              override def accept(t: OutputFrame): Unit = println(t.getUtf8String)
            })
            .withCommand("postgres", "-c", "log_statement=all", "-c", "log_destination=stderr")
        }

        container.start()
        container
      }.orDie)(container => attemptBlockingIO(container.stop()).orDie)
    }

}
