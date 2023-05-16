package io.iohk.atala.connect.core.repository

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.ConnectionRecord.*
import io.iohk.atala.connect.core.model.error.ConnectionRepositoryError.*
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.connection.{ConnectionRequest, ConnectionResponse}
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation
import zio.test.*
import zio.test.Assertion.*
import zio.{Cause, Exit, Task, ZIO}

import java.time.Instant
import java.util.UUID

object ConnectionRepositorySpecSuite {

  val maxRetries = 2

  private def connectionRecord = ConnectionRecord(
    UUID.randomUUID,
    Instant.ofEpochSecond(Instant.now.getEpochSecond),
    None,
    None,
    None,
    ConnectionRecord.Role.Inviter,
    ConnectionRecord.ProtocolState.InvitationGenerated,
    Invitation(
      id = UUID.randomUUID().toString,
      from = DidId("did:prism:aaa"),
      body = Invitation
        .Body(goal_code = "io.atalaprism.connect", goal = "Establish a trust connection between two peers", Nil)
    ),
    None,
    None,
    maxRetries,
    Some(Instant.now),
    None
  )

  private def connectionRequest = ConnectionRequest(
    from = DidId("did:prism:aaa"),
    to = DidId("did:prism:bbb"),
    thid = None,
    pthid = Some(UUID.randomUUID().toString),
    body = ConnectionRequest.Body(goal_code = Some("io.atalaprism.connect"))
  )

  val testSuite = suite("CRUD operations")(
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
        assertTrue(bCount match
          case Exit.Failure(cause: Cause.Fail[_]) if cause.value.isInstanceOf[UniqueConstraintViolation] => true
          case _                                                                                         => false
        )
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
        records <- repo.getConnectionRecords
      } yield {
        assertTrue(records.size == 2) &&
        assertTrue(records.contains(aRecord)) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("getConnectionRecordsByStates returns correct records") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        aRecord = connectionRecord
        bRecord = connectionRecord
        cRecord = connectionRecord
        _ <- repo.createConnectionRecord(aRecord)
        _ <- repo.createConnectionRecord(bRecord)
        _ <- repo.createConnectionRecord(cRecord)
        _ <- repo.updateConnectionProtocolState(
          aRecord.id,
          ProtocolState.InvitationGenerated,
          ProtocolState.ConnectionRequestReceived,
          1
        )
        _ <- repo.updateConnectionProtocolState(
          cRecord.id,
          ProtocolState.InvitationGenerated,
          ProtocolState.ConnectionResponsePending,
          1
        )
        invitationGeneratedRecords <- repo.getConnectionRecordsByStates(
          ignoreWithZeroRetries = true,
          ProtocolState.InvitationGenerated
        )
        otherRecords <- repo.getConnectionRecordsByStates(
          ignoreWithZeroRetries = true,
          ProtocolState.ConnectionRequestReceived,
          ProtocolState.ConnectionResponsePending
        )
      } yield {
        assertTrue(invitationGeneratedRecords.size == 1) &&
        assertTrue(invitationGeneratedRecords.contains(bRecord)) &&
        assertTrue(otherRecords.size == 2) &&
        assertTrue(otherRecords.exists(_.id == aRecord.id)) &&
        assertTrue(otherRecords.exists(_.id == cRecord.id))
      }
    },
    test("getConnectionRecordsByStates returns an empty list if 'states' parameter is empty") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        aRecord = connectionRecord
        bRecord = connectionRecord
        cRecord = connectionRecord
        _ <- repo.createConnectionRecord(aRecord)
        _ <- repo.createConnectionRecord(bRecord)
        _ <- repo.createConnectionRecord(cRecord)
        records <- repo.getConnectionRecordsByStates(ignoreWithZeroRetries = true)
      } yield {
        assertTrue(records.isEmpty)
      }
    },
    test("deleteRecord deletes an existing record") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        aRecord = connectionRecord
        bRecord = connectionRecord
        _ <- repo.createConnectionRecord(aRecord)
        _ <- repo.createConnectionRecord(bRecord)
        count <- repo.deleteConnectionRecord(aRecord.id)
        records <- repo.getConnectionRecords
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
        records <- repo.getConnectionRecords
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
    test("getConnectionRecordByThreadId returns nothing for an unknown thid") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        aRecord = connectionRecord.copy(thid = Some(UUID.randomUUID()))
        bRecord = connectionRecord.copy(thid = Some(UUID.randomUUID()))
        _ <- repo.createConnectionRecord(aRecord)
        _ <- repo.createConnectionRecord(bRecord)
        record <- repo.getConnectionRecordByThreadId(UUID.randomUUID())
      } yield assertTrue(record.isEmpty)
    },
    test("updateConnectionProtocolState updates the record") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        aRecord = connectionRecord
        _ <- repo.createConnectionRecord(aRecord)
        record <- repo.getConnectionRecord(aRecord.id)
        count <- repo.updateConnectionProtocolState(
          aRecord.id,
          ProtocolState.InvitationGenerated,
          ProtocolState.ConnectionRequestReceived,
          maxRetries
        )
        updatedRecord <- repo.getConnectionRecord(aRecord.id)
      } yield {
        assertTrue(count == 1) &&
        assertTrue(record.get.protocolState == ProtocolState.InvitationGenerated) &&
        assertTrue(updatedRecord.get.protocolState == ProtocolState.ConnectionRequestReceived)
      }
    },
    test("updateConnectionProtocolState doesn't update the record for invalid states") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        aRecord = connectionRecord
        _ <- repo.createConnectionRecord(aRecord)
        record <- repo.getConnectionRecord(aRecord.id)
        count <- repo.updateConnectionProtocolState(
          aRecord.id,
          ProtocolState.ConnectionRequestPending,
          ProtocolState.ConnectionRequestReceived,
          maxRetries
        )
        updatedRecord <- repo.getConnectionRecord(aRecord.id)
      } yield {
        assertTrue(count == 0) &&
        assertTrue(record.get.protocolState == ProtocolState.InvitationGenerated) &&
        assertTrue(updatedRecord.get.protocolState == ProtocolState.InvitationGenerated)
      }
    },
    test("updateWithConnectionRequest updates record") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        aRecord = connectionRecord
        _ <- repo.createConnectionRecord(aRecord)
        record <- repo.getConnectionRecord(aRecord.id)
        request = connectionRequest
        count <- repo.updateWithConnectionRequest(
          aRecord.id,
          request,
          ProtocolState.ConnectionRequestSent,
          maxRetries
        )
        updatedRecord <- repo.getConnectionRecord(aRecord.id)
      } yield {
        assertTrue(count == 1) &&
        assertTrue(record.get.connectionRequest.isEmpty) &&
        assertTrue(updatedRecord.get.connectionRequest.contains(request))
      }
    },
    test("updateWithConnectionResponse updates record") {
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        aRecord = connectionRecord
        _ <- repo.createConnectionRecord(aRecord)
        record <- repo.getConnectionRecord(aRecord.id)
        response = ConnectionResponse.makeResponseFromRequest(connectionRequest.makeMessage).toOption.get
        count <- repo.updateWithConnectionResponse(
          aRecord.id,
          response,
          ProtocolState.ConnectionResponseSent,
          maxRetries
        )
        updatedRecord <- repo.getConnectionRecord(aRecord.id)
      } yield {
        assertTrue(count == 1) &&
        assertTrue(record.get.connectionResponse.isEmpty) &&
        assertTrue(updatedRecord.get.connectionResponse.contains(response))
      }
    },
    test("updateFail (fail one retry) updates record") {
      val failReason = Some("Just to test")
      for {
        repo <- ZIO.service[ConnectionRepository[Task]]
        aRecord = connectionRecord
        _ <- repo.createConnectionRecord(aRecord)
        record <- repo.getConnectionRecord(aRecord.id)
        count <- repo.updateAfterFail(aRecord.id, Some("Just to test")) // TEST
        updatedRecord1 <- repo.getConnectionRecord(aRecord.id)
        response = ConnectionResponse.makeResponseFromRequest(connectionRequest.makeMessage).toOption.get
        count <- repo.updateWithConnectionResponse(
          aRecord.id,
          response,
          ProtocolState.ConnectionResponseSent,
          maxRetries
        )
        updatedRecord2 <- repo.getConnectionRecord(aRecord.id)
      } yield {
        assertTrue(record.get.metaRetries == maxRetries) &&
        assertTrue(updatedRecord1.get.metaRetries == (maxRetries - 1)) &&
        assertTrue(updatedRecord1.get.metaLastFailure == failReason) &&
        assertTrue(updatedRecord2.get.metaRetries == maxRetries) &&
        assertTrue(updatedRecord2.get.metaLastFailure == None) &&
        // continues to work normally after retry
        assertTrue(count == 1) &&
        assertTrue(record.get.connectionResponse.isEmpty) &&
        assertTrue(updatedRecord2.get.connectionResponse.contains(response))
      }
    }
  )
}
