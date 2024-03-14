package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.connection.{ConnectionRequest, ConnectionResponse}
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.time.Duration
import java.util.UUID
import io.iohk.atala.connect.core.model.error.ConnectionServiceError.InvitationParsingError
import io.iohk.atala.connect.core.model.error.ConnectionServiceError.InvitationAlreadyReceived
import io.iohk.atala.connect.core.model.error.ConnectionServiceError.InvalidStateForOperation
import io.iohk.atala.connect.core.model.error.ConnectionServiceError.RecordIdNotFound

trait ConnectionService {

  def createConnectionInvitation(
      label: Option[String],
      goalCode: Option[String],
      goal: Option[String],
      pairwiseDID: DidId
  ): URIO[WalletAccessContext, ConnectionRecord]

  def receiveConnectionInvitation(
      invitation: String
  ): ZIO[WalletAccessContext, InvitationParsingError | InvitationAlreadyReceived, ConnectionRecord]

  def acceptConnectionInvitation(
      recordId: UUID,
      pairwiseDid: DidId
  ): ZIO[WalletAccessContext, RecordIdNotFound | InvalidStateForOperation, ConnectionRecord]

  def markConnectionRequestSent(
      recordId: UUID
  ): ZIO[WalletAccessContext, RecordIdNotFound | InvalidStateForOperation, ConnectionRecord]

  def receiveConnectionRequest(
      request: ConnectionRequest,
      expirationTime: Option[Duration]
  ): ZIO[WalletAccessContext, ConnectionServiceError, ConnectionRecord]

  def acceptConnectionRequest(
      recordId: UUID
  ): ZIO[WalletAccessContext, ConnectionServiceError, ConnectionRecord]

  def markConnectionResponseSent(
      recordId: UUID
  ): ZIO[WalletAccessContext, ConnectionServiceError, ConnectionRecord]

  def markConnectionInvitationExpired(
      recordId: UUID
  ): ZIO[WalletAccessContext, ConnectionServiceError, ConnectionRecord]

  def receiveConnectionResponse(
      response: ConnectionResponse
  ): ZIO[WalletAccessContext, ConnectionServiceError, ConnectionRecord]

  def getConnectionRecords(): URIO[WalletAccessContext, Seq[ConnectionRecord]]

  def getConnectionRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: ConnectionRecord.ProtocolState*
  ): URIO[WalletAccessContext, Seq[ConnectionRecord]]

  def getConnectionRecordsByStatesForAllWallets(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: ConnectionRecord.ProtocolState*
  ): UIO[Seq[ConnectionRecord]]

  def getConnectionRecord(
      recordId: UUID
  ): URIO[WalletAccessContext, Option[ConnectionRecord]]

  def getConnectionRecordByThreadId(
      thid: String
  ): URIO[WalletAccessContext, Option[ConnectionRecord]]

  def deleteConnectionRecord(
      recordId: UUID
  ): URIO[WalletAccessContext, Unit]

  def reportProcessingFailure(
      recordId: UUID,
      failReason: Option[String]
  ): URIO[WalletAccessContext, Unit]
}
