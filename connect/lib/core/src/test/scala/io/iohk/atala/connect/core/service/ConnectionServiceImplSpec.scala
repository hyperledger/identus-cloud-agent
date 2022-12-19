package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.model.ConnectionRecord._
import io.iohk.atala.connect.core.repository.ConnectionRepositoryInMemory

import zio._
import zio.test._
import zio.Scope
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.connect.core.model.ConnectionRecord
import java.util.UUID

object ConnectionServiceImplSpec extends ZIOSpecDefault {

  val connectionServiceLayer = ConnectionRepositoryInMemory.layer >>> ConnectionServiceImpl.layer

  override def spec = {
    suite("ConnectionServiceImpl")(
      test("createConnectionInvitation creates a valid connection record") {
        for {
          svc <- ZIO.service[ConnectionService]
          did = DidId("did:peer:ABCDEF")
          record <- svc.createConnectionInvitation(Some("Test connection invitation"), did)
        } yield {
          assertTrue(record.label == Some("Test connection invitation")) &&
          assertTrue(record.protocolState == ProtocolState.InvitationGenerated) &&
          assertTrue(record.role == Role.Inviter) &&
          assertTrue(record.connectionResponse == None) &&
          assertTrue(record.connectionRequest == None) &&
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
              DidId("did:peer:ABCDEF")
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
              DidId("did:peer:ABCDEF")
            )
            createdRecord2 <- svc.createConnectionInvitation(
              Some("Test connection invitation #2"),
              DidId("did:peer:123456")
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
              DidId("did:peer:ABCDEF")
            )
            inviteeRecord <- inviteeSvc.createConnectionInvitation(
              Some("Invitee"),
              DidId("did:peer:ABCDEF")
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
        test("receiveConnectionInvitation should correctly create a new record") {
          for {
            inviterSvc <- ZIO.service[ConnectionService].provideLayer(connectionServiceLayer)
            inviteeSvc <- ZIO.service[ConnectionService].provideLayer(connectionServiceLayer)
            inviterRecord <- inviterSvc.createConnectionInvitation(
              Some("Test connection invitation"),
              DidId("did:peer:ABCDEF")
            )
            inviteeRecord <- inviteeSvc.receiveConnectionInvitation(inviterRecord.invitation.toBase64)
            allInviteeRecords <- inviteeSvc.getConnectionRecords()
          } yield {
            assertTrue(inviterRecord.invitation == inviteeRecord.invitation) &&
            assertTrue(allInviteeRecords.head == inviteeRecord)
          }
        }
      }
    ).provideLayer(connectionServiceLayer)
  }

}
