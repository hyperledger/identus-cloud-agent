package io.iohk.atala.connect.sql.repository

import cats.effect.std.Dispatcher
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.implicits._
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.ConnectionRecord._
import io.iohk.atala.connect.core.repository.ConnectionRepository
import io.iohk.atala.connect.core.repository.ConnectionRepositorySpecSuite
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.connection.ConnectionRequest
import io.iohk.atala.mercury.protocol.connection.ConnectionResponse
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation
import io.iohk.atala.test.container.PostgresTestContainer.*
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState
import zio._
import zio.interop.catz._
import zio.test.Assertion._
import zio.test._

import java.time.Instant
import java.util.UUID

object JdbcConnectionRepositorySpec extends ZIOSpecDefault {

  private val pgLayer = postgresLayer(verbose = false)
  private val dbConfig = ZLayer.fromZIO(
    for {
      postgres <- ZIO.service[PostgreSQLContainer]
    } yield DbConfig(postgres.username, postgres.password, postgres.jdbcUrl, 8, postgres.containerName)
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
    ) @@ TestAspect.before(
      ZIO.serviceWithZIO[Migrations](_.migrate)
    )).provide(testEnvironmentLayer)
}
