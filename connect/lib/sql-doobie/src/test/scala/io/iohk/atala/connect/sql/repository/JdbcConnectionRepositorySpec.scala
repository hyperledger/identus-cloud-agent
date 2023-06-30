package io.iohk.atala.connect.sql.repository

import cats.effect.std.Dispatcher
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.util.transactor.Transactor
import io.iohk.atala.connect.core.repository.ConnectionRepository
import io.iohk.atala.connect.core.repository.ConnectionRepositorySpecSuite
import io.iohk.atala.test.container.PostgresLayer.postgresLayer
import zio.*
import zio.interop.catz.*
import zio.test.*

object JdbcConnectionRepositorySpec extends ZIOSpecDefault {

  private val pgLayer = postgresLayer()
  private val dbConfig = ZLayer.fromZIO(
    for {
      postgres <- ZIO.service[PostgreSQLContainer]
    } yield DbConfig(postgres.username, postgres.password, postgres.jdbcUrl)
  )
  private val transactorLayer = ZLayer.fromZIO {
    ZIO.service[DbConfig].flatMap { config =>
      Dispatcher[Task].allocated.map { case (dispatcher, _) =>
        given Dispatcher[Task] = dispatcher
        TransactorLayer.hikari[Task](config)
      }
    }
  }.flatten
  private val testEnvironmentLayer = zio.test.testEnvironment ++ pgLayer ++
    (pgLayer >>> dbConfig >>> transactorLayer >>> JdbcConnectionRepository.layer) ++
    (pgLayer >>> dbConfig >>> Migrations.layer)

  override def spec: Spec[TestEnvironment with Scope, Any] =
    (suite("JDBC Connection Repository test suite")(
      ConnectionRepositorySpecSuite.testSuite
    ) @@ TestAspect.sequential @@ TestAspect.before(
      ZIO.serviceWithZIO[Migrations](_.migrate)
    )).provide(testEnvironmentLayer)
}
