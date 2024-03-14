package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
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
  ): ZIO[WalletAccessContext, ConnectionServiceError, ConnectionRecord]

  def receiveConnectionInvitation(
      invitation: String
  ): ZIO[WalletAccessContext, ConnectionServiceError, ConnectionRecord]

  def acceptConnectionInvitation(
      recordId: UUID,
      pairwiseDid: DidId
  ): ZIO[WalletAccessContext, ConnectionServiceError, ConnectionRecord]

  def markConnectionRequestSent(
      recordId: UUID
  ): ZIO[WalletAccessContext, ConnectionServiceError, ConnectionRecord]

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

  def getConnectionRecords(): ZIO[WalletAccessContext, ConnectionServiceError, Seq[ConnectionRecord]]

  def getConnectionRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: ConnectionRecord.ProtocolState*
  ): ZIO[WalletAccessContext, ConnectionServiceError, Seq[ConnectionRecord]]

  def getConnectionRecordsByStatesForAllWallets(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: ConnectionRecord.ProtocolState*
  ): IO[ConnectionServiceError, Seq[ConnectionRecord]]

  def getConnectionRecord(
      recordId: UUID
  ): ZIO[WalletAccessContext, ConnectionServiceError, Option[ConnectionRecord]]

  def getConnectionRecordByThreadId(
      thid: String
  ): ZIO[WalletAccessContext, ConnectionServiceError, Option[ConnectionRecord]]

  def deleteConnectionRecord(
      recordId: UUID
  ): ZIO[WalletAccessContext, ConnectionServiceError, Int]

  def reportProcessingFailure(
      recordId: UUID,
      failReason: Option[String]
  ): ZIO[WalletAccessContext, ConnectionServiceError, Unit]
}
