package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.model.ConnectionRecord._
import io.iohk.atala.connect.core.repository.ConnectionRepositoryInMemory

import zio._
import zio.test._
import zio.test.Assertion._
import zio.Scope
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.connect.core.model.ConnectionRecord
import java.util.UUID
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import java.time.Instant

object ConnectionServiceImplSpec extends ZIOSpecDefault {

  val connectionServiceLayer = ConnectionRepositoryInMemory.layer >>> ConnectionServiceImpl.layer

  override def spec = {
    suite("ConnectionServiceImpl")(
      test("createConnectionInvitation creates a valid inviter connection record") {
        for {
          svc <- ZIO.service[ConnectionService]
          did = DidId("did:peer:INVITER")
          record <- svc.createConnectionInvitation(Some("Test connection invitation"), did)
        } yield {
          assertTrue(record.label == Some("Test connection invitation")) &&
          assertTrue(record.protocolState == ProtocolState.InvitationGenerated) &&
          assertTrue(record.role == Role.Inviter) &&
          assertTrue(record.connectionRequest == None) &&
          assertTrue(record.connectionResponse == None) &&
          assertTrue(record.thid == Some(record.id)) &&
          assertTrue(record.updatedAt == None) &&
          assertTrue(record.invitation.from == did) &&
          assertTrue(record.invitation.attachments == None) &&
          assertTrue(record.invitation.body.goal_code == "connect") &&
          assertTrue(record.invitation.body.accept == Seq.empty)
        }
      }, {
        test("getConnectionRecord correctly returns record") {
          for {
            svc <- ZIO.service[ConnectionService]
            createdRecord <- svc.createConnectionInvitation(
              Some("Test connection invitation"),
              DidId("did:peer:INVITER")
            )
            foundRecord <- svc.getConnectionRecord(createdRecord.id)
            notFoundRecord <- svc.getConnectionRecord(UUID.randomUUID)
          } yield {
            assertTrue(foundRecord == Some(createdRecord)) &&
            assertTrue(notFoundRecord == None)
          }
        }
      }, {
        test("getConnectionRecords correctly returns all records") {
          for {
            svc <- ZIO.service[ConnectionService]
            createdRecord1 <- svc.createConnectionInvitation(
              Some("Test connection invitation #1"),
              DidId("did:peer:INVITER")
            )
            createdRecord2 <- svc.createConnectionInvitation(
              Some("Test connection invitation #2"),
              DidId("did:peer:INVITER")
            )
            records <- svc.getConnectionRecords()
          } yield {
            assertTrue(records.size == 2) &&
            assertTrue(records.contains(createdRecord1)) &&
            assertTrue(records.contains(createdRecord2))
          }
        }
      }, {
        test("scoped layers do not mix data") {
          for {
            inviterSvc <- ZIO.service[ConnectionService].provideLayer(connectionServiceLayer)
            inviteeSvc <- ZIO.service[ConnectionService].provideLayer(connectionServiceLayer)
            inviterRecord <- inviterSvc.createConnectionInvitation(
              Some("Inviter"),
              DidId("did:peer:INVITER")
            )
            inviteeRecord <- inviteeSvc.createConnectionInvitation(
              Some("Invitee"),
              DidId("did:peer:INVITEE")
            )
            allInviterRecords <- inviterSvc.getConnectionRecords()
            allInviteeRecords <- inviteeSvc.getConnectionRecords()
          } yield {
            assertTrue(allInviterRecords.size == 1) &&
            assertTrue(allInviterRecords.head == inviterRecord) &&
            assertTrue(allInviteeRecords.size == 1) &&
            assertTrue(allInviteeRecords.head == inviteeRecord)
          }
        }
      }, {
        test("receiveConnectionInvitation should correctly create a new invitee record") {
          for {
            inviterSvc <- ZIO.service[ConnectionService].provideLayer(connectionServiceLayer)
            inviteeSvc <- ZIO.service[ConnectionService].provideLayer(connectionServiceLayer)
            inviterRecord <- inviterSvc.createConnectionInvitation(
              Some("Test connection invitation"),
              DidId("did:peer:INVITER")
            )
            inviteeRecord <- inviteeSvc.receiveConnectionInvitation(inviterRecord.invitation.toBase64)
            allInviteeRecords <- inviteeSvc.getConnectionRecords()
          } yield {
            assertTrue(allInviteeRecords.head == inviteeRecord) &&
            assertTrue(inviteeRecord.label.isEmpty) &&
            assertTrue(inviteeRecord.protocolState == ProtocolState.InvitationReceived) &&
            assertTrue(inviteeRecord.role == Role.Invitee) &&
            assertTrue(inviteeRecord.connectionRequest.isEmpty) &&
            assertTrue(inviteeRecord.connectionResponse.isEmpty) &&
            assertTrue(inviteeRecord.thid == Some(UUID.fromString(inviterRecord.invitation.id))) &&
            assertTrue(inviteeRecord.updatedAt.isEmpty) &&
            assertTrue(inviteeRecord.invitation == inviterRecord.invitation)
          }
        }
      }, {
        test("acceptConnectionInvitation should return an error for an unknown recordId") {
          for {
            inviteeSvc <- ZIO.service[ConnectionService].provideLayer(connectionServiceLayer)
            unknownRecordId = UUID.randomUUID()
            inviteeRecord <- inviteeSvc.acceptConnectionInvitation(unknownRecordId, DidId("did:peer:INVITEE")).exit
          } yield {
            assert(inviteeRecord)(fails(equalTo(ConnectionServiceError.RecordIdNotFound(unknownRecordId))))
          }
        }
      }, {
        test("acceptConnectionInvitation should update the invitee record accordingly") {
          for {
            inviterSvc <- ZIO.service[ConnectionService].provideLayer(connectionServiceLayer)
            inviteeSvc <- ZIO.service[ConnectionService].provideLayer(connectionServiceLayer)
            inviterRecord <- inviterSvc.createConnectionInvitation(
              Some("Test connection invitation"),
              DidId("did:peer:INVITER")
            )
            inviteeRecord <- inviteeSvc.receiveConnectionInvitation(inviterRecord.invitation.toBase64)
            maybeInviteeRecord <- inviteeSvc.acceptConnectionInvitation(
              inviteeRecord.id,
              DidId("did:peer:INVITEE")
            )
            allInviteeRecords <- inviteeSvc.getConnectionRecords()
          } yield {
            val updatedRecord = maybeInviteeRecord.get
            assertTrue(allInviteeRecords.head == updatedRecord) &&
            assertTrue(updatedRecord.updatedAt.isDefined) &&
            assertTrue(updatedRecord.protocolState == ProtocolState.ConnectionRequestPending) &&
            assertTrue(updatedRecord.connectionRequest.isDefined)
          }
        }
      }, {
        test("receiveConnectionRequest should update the inviter record accordingly") {
          for {
            inviterSvc <- ZIO.service[ConnectionService].provideLayer(connectionServiceLayer)
            inviteeSvc <- ZIO.service[ConnectionService].provideLayer(connectionServiceLayer)
            inviterRecord <- inviterSvc.createConnectionInvitation(
              Some("Test connection invitation"),
              DidId("did:peer:INVITER")
            )
            inviteeRecord <- inviteeSvc.receiveConnectionInvitation(inviterRecord.invitation.toBase64)
            maybeAcceptedInvitationRecord <- inviteeSvc.acceptConnectionInvitation(
              inviteeRecord.id,
              DidId("did:peer:INVITEE")
            )
            // FIXME: Should the service return an Option while we have dedicated "not found" error for that case !?
            connectionRequest = maybeAcceptedInvitationRecord.get.connectionRequest.get
            maybeReceivedRequestConnectionRecord <- inviterSvc.receiveConnectionRequest(connectionRequest)
            allInviterRecords <- inviterSvc.getConnectionRecords()
          } yield {
            val updatedRecord = maybeReceivedRequestConnectionRecord.get
            assertTrue(allInviterRecords.head == updatedRecord) &&
            assertTrue(updatedRecord.updatedAt.isDefined) &&
            assertTrue(updatedRecord.protocolState == ProtocolState.ConnectionRequestReceived) &&
            assertTrue(updatedRecord.connectionRequest.isDefined) &&
            assertTrue(updatedRecord.connectionResponse.isEmpty)
          }
        }
      }, {
        test("acceptingConnectionRequest should update the inviter record accordingly") {
          for {
            inviterSvc <- ZIO.service[ConnectionService].provideLayer(connectionServiceLayer)
            inviteeSvc <- ZIO.service[ConnectionService].provideLayer(connectionServiceLayer)
            inviterRecord <- inviterSvc.createConnectionInvitation(
              Some("Test connection invitation"),
              DidId("did:peer:INVITER")
            )
            inviteeRecord <- inviteeSvc.receiveConnectionInvitation(inviterRecord.invitation.toBase64)
            maybeAcceptedInvitationRecord <- inviteeSvc.acceptConnectionInvitation(
              inviteeRecord.id,
              DidId("did:peer:INVITEE")
            )
            connectionRequest = maybeAcceptedInvitationRecord.get.connectionRequest.get
            maybeReceivedRequestConnectionRecord <- inviterSvc.receiveConnectionRequest(connectionRequest)
            maybeAcceptedRequestConnectionRecord <- inviterSvc.acceptConnectionRequest(inviterRecord.id)
            allInviterRecords <- inviterSvc.getConnectionRecords()
          } yield {
            val updatedRecord = maybeAcceptedRequestConnectionRecord.get
            assertTrue(allInviterRecords.head == updatedRecord) &&
            assertTrue(
              updatedRecord.updatedAt.forall(_.isAfter(maybeReceivedRequestConnectionRecord.get.updatedAt.get))
            ) &&
            assertTrue(updatedRecord.protocolState == ProtocolState.ConnectionResponsePending) &&
            assertTrue(updatedRecord.connectionRequest.isDefined) &&
            assertTrue(updatedRecord.connectionResponse.isDefined)
          }
        }
      }, {
        test("receiveConnectionResponse should update the invitee record accordingly") {
          for {
            inviterSvc <- ZIO.service[ConnectionService].provideLayer(connectionServiceLayer)
            inviteeSvc <- ZIO.service[ConnectionService].provideLayer(connectionServiceLayer)
            inviterRecord <- inviterSvc.createConnectionInvitation(
              Some("Test connection invitation"),
              DidId("did:peer:INVITER")
            )
            inviteeRecord <- inviteeSvc.receiveConnectionInvitation(inviterRecord.invitation.toBase64)
            maybeAcceptedInvitationRecord <- inviteeSvc.acceptConnectionInvitation(
              inviteeRecord.id,
              DidId("did:peer:INVITEE")
            )
            connectionRequest = maybeAcceptedInvitationRecord.get.connectionRequest.get
            _ <- inviteeSvc.markConnectionRequestSent(inviteeRecord.id)
            maybeReceivedRequestConnectionRecord <- inviterSvc.receiveConnectionRequest(connectionRequest)
            maybeAcceptedRequestConnectionRecord <- inviterSvc.acceptConnectionRequest(inviterRecord.id)
            connectionResponse = maybeAcceptedRequestConnectionRecord.get.connectionResponse.get
            _ <- inviterSvc.markConnectionResponseSent(inviterRecord.id)
            maybeReceivedResponseConnectionRecord <- inviteeSvc.receiveConnectionResponse(connectionResponse)
            allInviteeRecords <- inviteeSvc.getConnectionRecords()
          } yield {
            val updatedRecord = maybeReceivedResponseConnectionRecord.get
            assertTrue(allInviteeRecords.head == updatedRecord) &&
            assertTrue(
              updatedRecord.updatedAt.forall(_.isAfter(maybeAcceptedInvitationRecord.get.updatedAt.get))
            ) &&
            assertTrue(updatedRecord.protocolState == ProtocolState.ConnectionResponseReceived) &&
            assertTrue(updatedRecord.connectionRequest.isDefined) &&
            assertTrue(updatedRecord.connectionResponse.isDefined)
          }
        }
      }
    ).provideLayer(connectionServiceLayer)
  }

}
