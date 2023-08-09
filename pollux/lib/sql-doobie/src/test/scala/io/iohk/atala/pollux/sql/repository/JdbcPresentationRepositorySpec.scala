package io.iohk.atala.pollux.sql.repository

import com.dimafeng.testcontainers.PostgreSQLContainer
import io.iohk.atala.pollux.core.repository._
import io.iohk.atala.shared.db.DbConfig
import io.iohk.atala.shared.db.TransactorLayer
import io.iohk.atala.test.container.PostgresLayer.*
import zio._
import zio.test._

object JdbcPresentationRepositorySpec extends ZIOSpecDefault {

  private val pgLayer = postgresLayer(verbose = false)
  private val dbConfig = ZLayer.fromZIO(
    for {
      postgres <- ZIO.service[PostgreSQLContainer]
    } yield DbConfig(postgres.username, postgres.password, postgres.jdbcUrl)
  )
  private val transactorLayer = TransactorLayer.task
  private val testEnvironmentLayer = zio.test.testEnvironment ++ pgLayer ++
    (pgLayer >>> dbConfig >>> transactorLayer >>> JdbcPresentationRepository.layer) ++
    (pgLayer >>> dbConfig >>> Migrations.layer)

  override def spec =
    (suite("JDBC Presentation Repository test suite")(
      PresentationRepositorySpecSuite.testSuite
    ) @@ TestAspect.before(
      ZIO.serviceWithZIO[Migrations](_.migrate)
    )).provide(testEnvironmentLayer)
      .provide(Runtime.removeDefaultLoggers)

}
