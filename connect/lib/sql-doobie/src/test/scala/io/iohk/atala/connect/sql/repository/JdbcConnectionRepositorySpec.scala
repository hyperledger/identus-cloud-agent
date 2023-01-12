package io.iohk.atala.connect.sql.repository

import zio.test._
import zio._
import io.iohk.atala.connect.core.repository.ConnectionRepository
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import doobie.implicits._
import doobie.util.transactor.Transactor
import zio.interop.catz._
import io.iohk.atala.connect.core.model.ConnectionRecord
import java.util.UUID
import java.time.Instant
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation
import io.iohk.atala.mercury.model.DidId
import doobie.util.transactor.Transactor.Aux
import zio.test.Assertion._
import org.postgresql.util.PSQLException

object JdbcConnectionRepositorySpec extends ZIOSpecDefault {

  private val embeddedPostgres = ZLayer.fromZIO(
    ZIO.succeed(EmbeddedPostgres.builder().start())
  )

  private val dbConfig = ZLayer.fromZIO(
    for {
      postgres <- ZIO.service[EmbeddedPostgres]
    } yield DbConfig("postgres", "postgres", postgres.getJdbcUrl("postgres"))
  )

  private val transactor = ZLayer.fromZIO(
    for {
      dbConfig <- ZIO.service[DbConfig]
    } yield Transactor.fromDriverManager[Task](
      "org.postgresql.Driver",
      dbConfig.jdbcUrl,
      dbConfig.username,
      dbConfig.password
    )
  )

  private def connectionRecord = ConnectionRecord(
    UUID.randomUUID,
    Instant.ofEpochSecond(Instant.now.getEpochSecond()),
    None,
    None,
    None,
    ConnectionRecord.Role.Inviter,
    ConnectionRecord.ProtocolState.InvitationGenerated,
    Invitation(
      id = UUID.randomUUID().toString,
      from = DidId("did:prism:aaa"),
      body = Invitation.Body(goal_code = "connect", goal = "Establish a trust connection between two peers", Nil)
    ),
    None,
    None
  )

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("ConnectionServiceImpl")(
    test("createConnectionRecord creates a new record in DB") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        record = connectionRecord
        count <- repo.createConnectionRecord(record)
      } yield assertTrue(count == 1)
    },
    test("createConnectionRecord prevents creation of 2 records with the same thid") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        thid = UUID.randomUUID()
        aRecord = connectionRecord.copy(thid = Some(thid))
        bRecord = connectionRecord.copy(thid = Some(thid))
        aCount <- repo.createConnectionRecord(aRecord)
        bCount <- repo.createConnectionRecord(bRecord).exit
      } yield {
        assert(bCount)(fails(isSubtype[PSQLException](anything)))
      }
    },
    test("getConnectionRecord correctly returns an existing record") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        aRecord = connectionRecord
        bRecord = connectionRecord
        _ <- repo.createConnectionRecord(aRecord)
        _ <- repo.createConnectionRecord(bRecord)
        record <- repo.getConnectionRecord(bRecord.id)
      } yield assertTrue(record.contains(bRecord))
    },
    test("getConnectionRecord returns None for an unknown record") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        aRecord = connectionRecord
        bRecord = connectionRecord
        _ <- repo.createConnectionRecord(aRecord)
        _ <- repo.createConnectionRecord(bRecord)
        record <- repo.getConnectionRecord(UUID.randomUUID())
      } yield assertTrue(record.isEmpty)
    },
    test("getConnectionRecords returns all records") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        aRecord = connectionRecord
        bRecord = connectionRecord
        _ <- repo.createConnectionRecord(aRecord)
        _ <- repo.createConnectionRecord(bRecord)
        records <- repo.getConnectionRecords()
      } yield {
        assertTrue(records.size == 2) &&
        assertTrue(records.contains(aRecord)) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("deleteRecord deletes an exsiting record") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        aRecord = connectionRecord
        bRecord = connectionRecord
        _ <- repo.createConnectionRecord(aRecord)
        _ <- repo.createConnectionRecord(bRecord)
        count <- repo.deleteConnectionRecord(aRecord.id)
        records <- repo.getConnectionRecords()
      } yield {
        assertTrue(count == 1) &&
        assertTrue(records.size == 1) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("deleteRecord does nothing for an unknown record") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        aRecord = connectionRecord
        bRecord = connectionRecord
        _ <- repo.createConnectionRecord(aRecord)
        _ <- repo.createConnectionRecord(bRecord)
        count <- repo.deleteConnectionRecord(UUID.randomUUID)
        records <- repo.getConnectionRecords()
      } yield {
        assertTrue(count == 0) &&
        assertTrue(records.size == 2) &&
        assertTrue(records.contains(aRecord)) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("getConnectionRecordByThreadId correctly returns an existing thid") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        thid = UUID.randomUUID()
        aRecord = connectionRecord.copy(thid = Some(thid))
        bRecord = connectionRecord
        _ <- repo.createConnectionRecord(aRecord)
        _ <- repo.createConnectionRecord(bRecord)
        record <- repo.getConnectionRecordByThreadId(thid)
      } yield assertTrue(record.contains(aRecord))
    },
    test("getConnectionRecordByThreadId returns nowthing for an unknown thid") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        aRecord = connectionRecord.copy(thid = Some(UUID.randomUUID()))
        bRecord = connectionRecord.copy(thid = Some(UUID.randomUUID()))
        _ <- repo.createConnectionRecord(aRecord)
        _ <- repo.createConnectionRecord(bRecord)
        record <- repo.getConnectionRecordByThreadId(UUID.randomUUID())
      } yield assertTrue(record.isEmpty)
    }
  ).provide(
    embeddedPostgres,
    dbConfig,
    transactor,
    JdbcConnectionRepository.layer,
    Migrations.layer >>> ZLayer.fromZIO(ZIO.serviceWithZIO[Migrations](_.migrate))
  )
}
