package io.iohk.atala.connect.sql.repository

import com.dimafeng.testcontainers.PostgreSQLContainer
import io.iohk.atala.connect.core.repository.{ConnectionRepository, ConnectionRepositorySpecSuite}
import io.iohk.atala.shared.db.DbConfig
import io.iohk.atala.shared.test.containers.PostgresTestContainerSupport
import zio.*
import zio.test.*

object JdbcConnectionRepositorySpec extends ZIOSpecDefault, PostgresTestContainerSupport {

  private val dbConfig = ZLayer.fromZIO(
    for {
      postgres <- ZIO.service[PostgreSQLContainer]
    } yield DbConfig(postgres.username, postgres.password, postgres.jdbcUrl)
  )

  private val testEnvironmentLayer = ZLayer.make[ConnectionRepository & Migrations](
    JdbcConnectionRepository.layer,
    Migrations.layer,
    dbConfig,
    pgContainerLayer,
    contextAwareTransactorLayer,
    systemTransactorLayer
  )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    (suite("JDBC Connection Repository test suite")(
      ConnectionRepositorySpecSuite.testSuite,
      ConnectionRepositorySpecSuite.multitenantTestSuite
    ) @@ TestAspect.sequential @@ TestAspect.before(
      ZIO.serviceWithZIO[Migrations](_.migrate)
    )).provide(testEnvironmentLayer)
      .provide(Runtime.removeDefaultLoggers)
}
