package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.connection.ConnectionRequest
import io.iohk.atala.mercury.protocol.connection.ConnectionResponse
import io.iohk.atala.shared.models.WalletAccessContext
import zio.URIO
import zio.UIO
import zio.URLayer
import zio.ZIO
import zio.ZLayer
import zio.mock
import zio.mock.Mock
import zio.mock.Proxy

import java.time.Duration
import java.util.UUID
import io.iohk.atala.connect.core.model.error.ConnectionServiceError.InvitationAlreadyReceived
import io.iohk.atala.connect.core.model.error.ConnectionServiceError.InvitationParsingError
import io.iohk.atala.connect.core.model.error.ConnectionServiceError.InvalidStateForOperation
import io.iohk.atala.connect.core.model.error.ConnectionServiceError.RecordIdNotFound

object MockConnectionService extends Mock[ConnectionService] {

  object CreateConnectionInvitation
      extends Effect[(Option[String], Option[String], Option[String], DidId), Nothing, ConnectionRecord]
  object ReceiveConnectionInvitation
      extends Effect[String, InvitationParsingError | InvitationAlreadyReceived, ConnectionRecord]
  object AcceptConnectionInvitation extends Effect[(UUID, DidId), RecordIdNotFound | InvalidStateForOperation, ConnectionRecord]
  object MarkConnectionRequestSent extends Effect[UUID, RecordIdNotFound | InvalidStateForOperation, ConnectionRecord]
  object ReceiveConnectionRequest extends Effect[ConnectionRequest, ConnectionServiceError, ConnectionRecord]
  object AcceptConnectionRequest extends Effect[UUID, ConnectionServiceError, ConnectionRecord]
  object MarkConnectionResponseSent extends Effect[UUID, ConnectionServiceError, ConnectionRecord]
  object MarkConnectionInvitationExpired extends Effect[UUID, ConnectionServiceError, ConnectionRecord]

  object ReceiveConnectionResponse extends Effect[ConnectionResponse, ConnectionServiceError, ConnectionRecord]

  override val compose: URLayer[mock.Proxy, ConnectionService] = ZLayer {
    for {
      proxy <- ZIO.service[Proxy]
    } yield new ConnectionService {
      override def createConnectionInvitation(
          label: Option[String],
          goalCode: Option[String],
          goal: Option[String],
          pairwiseDID: DidId
      ): URIO[WalletAccessContext, ConnectionRecord] =
        proxy(CreateConnectionInvitation, label, goalCode, goal, pairwiseDID)

      override def receiveConnectionInvitation(
          invitation: String
      ): ZIO[WalletAccessContext, InvitationParsingError | InvitationAlreadyReceived, ConnectionRecord] =
        proxy(ReceiveConnectionInvitation, invitation)

      override def acceptConnectionInvitation(
          recordId: UUID,
          pairwiseDid: DidId
      ): ZIO[WalletAccessContext, RecordIdNotFound | InvalidStateForOperation, ConnectionRecord] =
        proxy(AcceptConnectionInvitation, recordId, pairwiseDid)

      override def markConnectionRequestSent(
          recordId: UUID
      ): ZIO[WalletAccessContext, RecordIdNotFound | InvalidStateForOperation, ConnectionRecord] =
        proxy(MarkConnectionRequestSent, recordId)

      override def receiveConnectionRequest(
          request: ConnectionRequest,
          expirationTime: Option[Duration]
      ): ZIO[WalletAccessContext, ConnectionServiceError, ConnectionRecord] =
        proxy(ReceiveConnectionRequest, request)

      override def acceptConnectionRequest(
          recordId: UUID
      ): ZIO[WalletAccessContext, ConnectionServiceError, ConnectionRecord] =
        proxy(AcceptConnectionRequest, recordId)

      override def markConnectionInvitationExpired(
          recordId: UUID
      ): ZIO[WalletAccessContext, ConnectionServiceError, ConnectionRecord] =
        proxy(MarkConnectionInvitationExpired, recordId)

      override def markConnectionResponseSent(
          recordId: UUID
      ): ZIO[WalletAccessContext, ConnectionServiceError, ConnectionRecord] =
        proxy(MarkConnectionResponseSent, recordId)

      override def receiveConnectionResponse(
          response: ConnectionResponse
      ): ZIO[WalletAccessContext, ConnectionServiceError, ConnectionRecord] =
        proxy(ReceiveConnectionResponse, response)

      override def getConnectionRecords(): URIO[WalletAccessContext, Seq[ConnectionRecord]] = ???

      override def getConnectionRecordsByStates(
          ignoreWithZeroRetries: Boolean,
          limit: Int,
          states: ConnectionRecord.ProtocolState*
      ): URIO[WalletAccessContext, Seq[ConnectionRecord]] = ???

      override def getConnectionRecordsByStatesForAllWallets(
          ignoreWithZeroRetries: Boolean,
          limit: Int,
          states: ConnectionRecord.ProtocolState*
      ): UIO[Seq[ConnectionRecord]] = ???

      override def getConnectionRecord(
          recordId: UUID
      ): URIO[WalletAccessContext, Option[ConnectionRecord]] = ???

      override def getConnectionRecordByThreadId(
          thid: String
      ): URIO[WalletAccessContext, Option[ConnectionRecord]] =
        ???

      override def deleteConnectionRecord(
          recordId: UUID
      ): URIO[WalletAccessContext, Unit] = ???

      override def reportProcessingFailure(
          recordId: UUID,
          failReason: Option[String]
      ): URIO[WalletAccessContext, Unit] = ???
    }
  }
}
