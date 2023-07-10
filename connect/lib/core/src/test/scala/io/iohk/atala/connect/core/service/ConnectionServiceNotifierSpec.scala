package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.ConnectionRecord.ProtocolState
import io.iohk.atala.connect.core.repository.ConnectionRepositoryInMemory
import io.iohk.atala.event.notification.*
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.connection.{ConnectionRequest, ConnectionResponse}
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation
import zio.*
import zio.ZIO.*
import zio.mock.Expectation
import zio.test.*

import java.time.Instant
import java.util.UUID

object ConnectionServiceNotifierSpec extends ZIOSpecDefault {

  private val record = ConnectionRecord(
    UUID.randomUUID(),
    Instant.now,
    None,
    UUID.randomUUID().toString,
    None,
    ConnectionRecord.Role.Inviter,
    ProtocolState.InvitationGenerated,
    Invitation(from = DidId("did:peer:INVITER"), Invitation.Body("", "", Nil)),
    None,
    None,
    5,
    None,
    None
  )

  private val inviterExpectations =
    MockConnectionService.CreateConnectionInvitation(
      assertion = Assertion.anything,
      result = Expectation.value(record)
    ) ++ MockConnectionService.ReceiveConnectionRequest(
      assertion = Assertion.anything,
      result = Expectation.value(record.copy(protocolState = ProtocolState.ConnectionRequestReceived))
    ) ++ MockConnectionService.AcceptConnectionRequest(
      assertion = Assertion.anything,
      result = Expectation.value(record.copy(protocolState = ProtocolState.ConnectionResponsePending))
    ) ++ MockConnectionService.MarkConnectionResponseSent(
      assertion = Assertion.anything,
      result = Expectation.value(record.copy(protocolState = ProtocolState.ConnectionResponseSent))
    )

  private val inviteeExpectations =
    MockConnectionService.ReceiveConnectionInvitation(
      assertion = Assertion.anything,
      result = Expectation.value(record.copy(protocolState = ProtocolState.InvitationReceived))
    ) ++ MockConnectionService.AcceptConnectionInvitation(
      assertion = Assertion.anything,
      result = Expectation.value(record.copy(protocolState = ProtocolState.ConnectionRequestPending))
    ) ++ MockConnectionService.MarkConnectionRequestSent(
      assertion = Assertion.anything,
      result = Expectation.value(record.copy(protocolState = ProtocolState.ConnectionRequestSent))
    ) ++ MockConnectionService.ReceiveConnectionResponse(
      assertion = Assertion.anything,
      result = Expectation.value(record.copy(protocolState = ProtocolState.ConnectionResponseReceived))
    )

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ConnectionServiceWithEventNotificationImpl")(
      test("should send relevant events during flow execution on the inviter side") {
        for {
          cs <- ZIO.service[ConnectionService]
          ens <- ZIO.service[EventNotificationService]
          did = DidId("did:peer:INVITER")
          connectionRecord <- cs.createConnectionInvitation(Some("test"), did)
          _ <- cs.receiveConnectionRequest(
            ConnectionRequest(
              from = DidId("did:peer:INVITER"),
              to = DidId("did:peer:INVITEE"),
              thid = Some(connectionRecord.thid),
              pthid = None,
              body = ConnectionRequest.Body()
            )
          )
          _ <- cs.acceptConnectionRequest(connectionRecord.id)
          _ <- cs.markConnectionResponseSent(connectionRecord.id)
          consumer <- ens.consumer[ConnectionRecord]("Connect")
          events <- consumer.poll(50)
        } yield {
          assertTrue(events.size == 4) &&
          assertTrue(events.head.data.protocolState == ProtocolState.InvitationGenerated) &&
          assertTrue(events(1).data.protocolState == ProtocolState.ConnectionRequestReceived) &&
          assertTrue(events(2).data.protocolState == ProtocolState.ConnectionResponsePending) &&
          assertTrue(events(3).data.protocolState == ProtocolState.ConnectionResponseSent)
        }
      }.provide(
        ZLayer.succeed(50) >>> EventNotificationServiceImpl.layer,
        inviterExpectations.toLayer >>> ConnectionServiceNotifier.layer
      ),
      test("should send relevant events during flow execution on the invitee side") {
        for {
          inviterSvc <- ZIO
            .service[ConnectionService]
            .provideLayer(ConnectionRepositoryInMemory.layer >>> ConnectionServiceImpl.layer)
          inviterDID = DidId("did:peer:INVITER")
          inviterRecord <- inviterSvc.createConnectionInvitation(
            Some("Test connection invitation"),
            inviterDID
          )
          inviteeSvc <- ZIO.service[ConnectionService]
          inviteeDID = DidId("did:peer:INVITEE")
          ens <- ZIO.service[EventNotificationService]
          connectionRecord <- inviteeSvc.receiveConnectionInvitation(inviterRecord.invitation.toBase64)
          _ <- inviteeSvc.acceptConnectionInvitation(connectionRecord.id, inviteeDID)
          _ <- inviteeSvc.markConnectionRequestSent(connectionRecord.id)
          _ <- inviteeSvc.receiveConnectionResponse(
            ConnectionResponse(
              from = inviterDID,
              to = inviteeDID,
              thid = Some(connectionRecord.thid),
              pthid = None,
              body = ConnectionResponse.Body()
            )
          )
          consumer <- ens.consumer[ConnectionRecord]("Connect")
          events <- consumer.poll(50)
        } yield {
          assertTrue(events.size == 4) &&
          assertTrue(events.head.data.protocolState == ProtocolState.InvitationReceived) &&
          assertTrue(events(1).data.protocolState == ProtocolState.ConnectionRequestPending) &&
          assertTrue(events(2).data.protocolState == ProtocolState.ConnectionRequestSent) &&
          assertTrue(events(3).data.protocolState == ProtocolState.ConnectionResponseReceived)
        }
      }.provide(
        ZLayer.succeed(50) >>> EventNotificationServiceImpl.layer,
        inviteeExpectations.toLayer >>> ConnectionServiceNotifier.layer
      )
    )
  }

}
