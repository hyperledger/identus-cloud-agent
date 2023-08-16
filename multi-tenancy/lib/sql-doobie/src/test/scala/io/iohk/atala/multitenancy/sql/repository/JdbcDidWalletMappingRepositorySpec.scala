package io.iohk.atala.multitenancy.sql.repository

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.util.transactor.Transactor
import io.iohk.atala.multitenancy.core.repository.{DidWalletMappingRepository, DidWalletMappingRepositorySpecSuite}
import io.iohk.atala.shared.db.ContextAwareTask
import io.iohk.atala.test.container.PostgresLayer.postgresLayer
import zio.*
import zio.interop.catz.*
import zio.test.*

object JdbcDidWalletMappingRepositorySpec extends ZIOSpecDefault {

  private val pgLayer = postgresLayer()
  private val dbConfig = ZLayer.fromZIO(
    for {
      postgres <- ZIO.service[PostgreSQLContainer]
    } yield DbConfig(postgres.username, postgres.password, postgres.jdbcUrl)
  )

  private val transactorLayer = ZLayer.fromZIO {
    ZIO.service[DbConfig].flatMap { config =>
      given Async[ContextAwareTask] = summon[Async[Task]].asInstanceOf
      Dispatcher[ContextAwareTask].allocated.map { case (dispatcher, _) =>
        given Dispatcher[ContextAwareTask] = dispatcher
        TransactorLayer.hikari[ContextAwareTask](config)
      }
    }
  }.flatten

  private val testEnvironmentLayer = zio.test.testEnvironment ++ pgLayer ++
    (pgLayer >>> dbConfig >>> transactorLayer >>> JdbcDidWalletMappingRepository.layer) ++
    (pgLayer >>> dbConfig >>> Migrations.layer)

  override def spec: Spec[TestEnvironment with Scope, Any] =
    (suite("JDBC Did Wallet Mapping Repository test suite")(
      DidWalletMappingRepositorySpecSuite.testSuite
    ) @@ TestAspect.sequential @@ TestAspect.before(
      ZIO.serviceWithZIO[Migrations](_.migrate)
    )).provide(testEnvironmentLayer)
      .provide(Runtime.removeDefaultLoggers)
}
