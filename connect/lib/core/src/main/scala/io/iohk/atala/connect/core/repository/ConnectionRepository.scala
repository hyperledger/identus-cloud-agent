package io.iohk.atala.connect.core.repository

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.ConnectionRecord.ProtocolState
import io.iohk.atala.mercury.protocol.connection.*
import io.iohk.atala.shared.models.WalletAccessContext
import zio.RIO
import zio.Task
import java.util.UUID

trait ConnectionRepository {
  def createConnectionRecord(record: ConnectionRecord): RIO[WalletAccessContext, Int]

  def getConnectionRecords: RIO[WalletAccessContext, Seq[ConnectionRecord]]

  def getConnectionRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: ConnectionRecord.ProtocolState*
  ): RIO[WalletAccessContext, Seq[ConnectionRecord]]

  def getConnectionRecordsByStatesForAllWallets(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: ConnectionRecord.ProtocolState*
  ): Task[Seq[ConnectionRecord]]

  def getConnectionRecord(recordId: UUID): RIO[WalletAccessContext, Option[ConnectionRecord]]

  def deleteConnectionRecord(recordId: UUID): RIO[WalletAccessContext, Int]

  def getConnectionRecordByThreadId(thid: String): RIO[WalletAccessContext, Option[ConnectionRecord]]

  def updateWithConnectionRequest(
      recordId: UUID,
      request: ConnectionRequest,
      state: ProtocolState,
      maxRetries: Int, // max numbre of retries -> set the metaRetries
  ): RIO[WalletAccessContext, Int]

  def updateWithConnectionResponse(
      recordId: UUID,
      response: ConnectionResponse,
      state: ProtocolState,
      maxRetries: Int, // max numbre of retries -> set the metaRetries
  ): RIO[WalletAccessContext, Int]

  def updateConnectionProtocolState(
      recordId: UUID,
      from: ProtocolState,
      to: ProtocolState,
      maxRetries: Int, // max numbre of retries -> set the metaRetries
  ): RIO[WalletAccessContext, Int]

  def updateAfterFail(
      recordId: UUID,
      failReason: Option[String],
  ): RIO[WalletAccessContext, Int]

}
