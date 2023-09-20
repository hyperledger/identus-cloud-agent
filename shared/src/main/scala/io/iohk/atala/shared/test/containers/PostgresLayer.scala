package io.iohk.atala.shared.test.containers

import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.util.transactor.Transactor
import io.iohk.atala.shared.db.ContextAwareTask
import io.iohk.atala.shared.db.DbConfig
import io.iohk.atala.shared.db.TransactorLayer
import io.iohk.atala.shared.test.containers.PostgresTestContainer.postgresContainer
import zio.*

object PostgresLayer {

  def postgresLayer(
      imageName: Option[String] = Some("postgres:13"),
      verbose: Boolean = false
  ): TaskLayer[PostgreSQLContainer] =
    ZLayer.scoped {
      ZIO
        .acquireRelease(ZIO.attemptBlockingIO {
          postgresContainer(imageName, verbose)
        })(container => ZIO.attemptBlockingIO(container.stop()).orDie)
        // Start the container outside the aquireRelease as this might fail
        // to ensure contianer.stop() is added to the finalizer
        .tap(container => ZIO.attemptBlocking(container.start()))
    }

  private def dbConfig(container: PostgreSQLContainer): DbConfig = {
    DbConfig(
      username = container.username,
      password = container.password,
      jdbcUrl = container.jdbcUrl
    )
  }

  lazy val dbConfigLayer: ZLayer[PostgreSQLContainer, Nothing, DbConfig] =
    ZLayer.fromZIO { ZIO.serviceWith[PostgreSQLContainer](dbConfig) }

  def transactor: ZLayer[DbConfig, Throwable, Transactor[ContextAwareTask]] = TransactorLayer.contextAwareTask
}
