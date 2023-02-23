package io.iohk.atala.pollux.sql.repository

import cats.effect.std.Dispatcher
import com.dimafeng.testcontainers.PostgreSQLContainer
import io.iohk.atala.pollux.core.model._
import io.iohk.atala.pollux.core.repository._
import io.iohk.atala.pollux.sql.repository.DbConfig
import io.iohk.atala.pollux.sql.repository.Migrations
import io.iohk.atala.test.container.PostgresTestContainer.*
import zio._
import zio.interop.catz._
import zio.test._
import java.util.UUID
import java.time.Instant

object JdbcCredentialRepositorySpec extends ZIOSpecDefault {

  private val pgLayer = postgresLayer(verbose = false)
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
    (pgLayer >>> dbConfig >>> transactorLayer >>> JdbcCredentialRepository.layer) ++
    (pgLayer >>> dbConfig >>> Migrations.layer)

  override def spec =
    (suite("JDBC Credential Repository test suite")(
      CredentialRepositorySpecSuite.testSuite
    ) @@ TestAspect.before(
      ZIO.serviceWithZIO[Migrations](_.migrate)
    )).provide(testEnvironmentLayer)

}
