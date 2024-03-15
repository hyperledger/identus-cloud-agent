package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.connect.core.model.error.ConnectionServiceError.*
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.connection.{ConnectionRequest, ConnectionResponse}
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.time.Duration
import java.util.UUID

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
  ): ZIO[WalletAccessContext, ThreadIdNotFound | InvalidStateForOperation | InvitationExpired, ConnectionRecord]

  def acceptConnectionRequest(
      recordId: UUID
  ): ZIO[WalletAccessContext, RecordIdNotFound | InvalidStateForOperation, ConnectionRecord]

  def markConnectionResponseSent(
      recordId: UUID
  ): ZIO[WalletAccessContext, RecordIdNotFound | InvalidStateForOperation, ConnectionRecord]

  def markConnectionInvitationExpired(
      recordId: UUID
  ): URIO[WalletAccessContext, ConnectionRecord]

  def receiveConnectionResponse(
      response: ConnectionResponse
  ): ZIO[WalletAccessContext, ThreadIdMissingInMessage | ThreadIdNotFound | InvalidStateForOperation, ConnectionRecord]

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
