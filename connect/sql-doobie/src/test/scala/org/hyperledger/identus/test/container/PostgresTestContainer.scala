package org.hyperledger.identus.test.container

import com.dimafeng.testcontainers.PostgreSQLContainer
import org.hyperledger.identus.sharedtest.containers.PostgresTestContainer.postgresContainer
import zio._
import zio.ZIO._

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
