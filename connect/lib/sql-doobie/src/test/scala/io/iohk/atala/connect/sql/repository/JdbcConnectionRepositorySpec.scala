package io.iohk.atala.connect.sql.repository

import com.dimafeng.testcontainers.PostgreSQLContainer
import io.iohk.atala.connect.core.repository.{ConnectionRepository, ConnectionRepositorySpecSuite}
import io.iohk.atala.shared.db.{DbConfig, TransactorLayer}
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.test.container.PostgresLayer.postgresLayer
import zio.*
import zio.test.*

object JdbcConnectionRepositorySpec extends ZIOSpecDefault {

  private val pgLayer = postgresLayer()
  private val dbConfig = ZLayer.fromZIO(
    for {
      postgres <- ZIO.service[PostgreSQLContainer]
    } yield DbConfig(postgres.username, postgres.password, postgres.jdbcUrl)
  )
  private val transactorLayer = TransactorLayer.contextAwareTask
  private val testEnvironmentLayer = zio.test.testEnvironment ++ pgLayer ++
    (pgLayer >>> dbConfig >>> transactorLayer >>> JdbcConnectionRepository.layer) ++
    (pgLayer >>> dbConfig >>> Migrations.layer)

  override def spec: Spec[TestEnvironment with Scope, Any] =
    (suite("JDBC Connection Repository test suite")(
      ConnectionRepositorySpecSuite.testSuite
    ) @@ TestAspect.sequential @@ TestAspect.before(
      ZIO.serviceWithZIO[Migrations](_.migrate)
    )).provide(testEnvironmentLayer)
      .provide(Runtime.removeDefaultLoggers)
}
