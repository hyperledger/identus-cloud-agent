package io.iohk.atala.pollux.sql.repository

import cats.effect.std.Dispatcher
import com.dimafeng.testcontainers.PostgreSQLContainer
import io.iohk.atala.pollux.core.model._
import io.iohk.atala.pollux.core.repository._
import io.iohk.atala.pollux.sql.repository.DbConfig
import io.iohk.atala.pollux.sql.repository.Migrations
import io.iohk.atala.test.container.PostgresTestContainer.*
import zio.Task
import zio.ZIO
import zio.ZLayer
import zio.interop.catz._
import zio.test._
import java.util.UUID
import java.time.Instant

/** sql-doobie/testOnly io.iohk.atala.pollux.sql.repository.JdbcCredentialRepositorySpec */
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

  def maxRetries = 2
  def aRecord = IssueCredentialRecord(
    id = UUID.randomUUID(),
    createdAt = Instant.now,
    updatedAt = None,
    thid = UUID.randomUUID(),
    schemaId = None,
    role = IssueCredentialRecord.Role.Issuer,
    subjectId = "subjectId",
    validityPeriod = None,
    automaticIssuance = None,
    awaitConfirmation = None,
    protocolState = IssueCredentialRecord.ProtocolState.OfferPending,
    publicationState = None,
    offerCredentialData = None,
    requestCredentialData = None,
    issueCredentialData = None,
    issuedCredentialRaw = None,
    issuingDID = None,
    metaRetries = maxRetries,
    metaNextRetry = Some(Instant.now()),
    metaLastFailure = None
  )

  test("updateFail (fail one retry) updates record") {
    val failReason = Some("Just to test")
    for {
      repo <- ZIO.service[CredentialRepository[Task]]
      _ <- repo.createIssueCredentialRecord(aRecord)
      record <- repo.getIssueCredentialRecord(aRecord.id)
      count <- repo.updateAfterFail(aRecord.id, Some("Just to test")) // TEST
      updatedRecord1 <- repo.getIssueCredentialRecord(aRecord.id)
      // response = ConnectionResponse.makeResponseFromRequest(connectionRequest.makeMessage)
      // count <- repo.updateWithConnectionResponse(
      //   aRecord.id,
      //   response,
      //   ProtocolState.ConnectionResponseSent,
      //   maxRetries
      // )
      updatedRecord2 <- repo.getIssueCredentialRecord(aRecord.id)
    } yield {
      assertTrue(record.get.metaRetries == maxRetries) &&
      assertTrue(updatedRecord1.get.metaRetries == (maxRetries - 1)) &&
      assertTrue(updatedRecord1.get.metaLastFailure == failReason) &&
      assertTrue(updatedRecord2.get.metaRetries == maxRetries) &&
      assertTrue(updatedRecord2.get.metaLastFailure == None) &&
      // continues to work normally after retry
      assertTrue(count == 1)
    }
  }
}
