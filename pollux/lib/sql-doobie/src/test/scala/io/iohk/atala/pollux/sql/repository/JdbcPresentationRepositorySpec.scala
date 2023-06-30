package io.iohk.atala.pollux.sql.repository

import cats.effect.std.Dispatcher
import com.dimafeng.testcontainers.PostgreSQLContainer
import io.iohk.atala.pollux.core.repository._
import io.iohk.atala.test.container.PostgresLayer.*
import zio._
import zio.interop.catz._
import zio.test._

object JdbcPresentationRepositorySpec extends ZIOSpecDefault {

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
    (pgLayer >>> dbConfig >>> transactorLayer >>> JdbcPresentationRepository.layer) ++
    (pgLayer >>> dbConfig >>> Migrations.layer)

  override def spec =
    (suite("JDBC Presentation Repository test suite")(
      PresentationRepositorySpecSuite.testSuite
    ) @@ TestAspect.before(
      ZIO.serviceWithZIO[Migrations](_.migrate)
    )).provide(testEnvironmentLayer)

}
