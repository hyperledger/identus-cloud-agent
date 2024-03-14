package io.iohk.atala.connect.core.repository

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.ConnectionRecord.ProtocolState
import io.iohk.atala.mercury.protocol.connection.*
import io.iohk.atala.shared.models.WalletAccessContext
import zio.UIO
import zio.URIO

import java.util.UUID

trait ConnectionRepository {

  def createConnectionRecord(
      record: ConnectionRecord
  ): URIO[WalletAccessContext, Unit]

  def getConnectionRecords: URIO[WalletAccessContext, Seq[ConnectionRecord]]

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

  def deleteConnectionRecord(
      recordId: UUID
  ): URIO[WalletAccessContext, Unit]

  def getConnectionRecordByThreadId(
      thid: String
  ): URIO[WalletAccessContext, Option[ConnectionRecord]]

  def updateWithConnectionRequest(
      recordId: UUID,
      request: ConnectionRequest,
      state: ProtocolState,
      maxRetries: Int,
  ): URIO[WalletAccessContext, Unit]

  def updateWithConnectionResponse(
      recordId: UUID,
      response: ConnectionResponse,
      state: ProtocolState,
      maxRetries: Int,
  ): URIO[WalletAccessContext, Unit]

  def updateConnectionProtocolState(
      recordId: UUID,
      from: ProtocolState,
      to: ProtocolState,
      maxRetries: Int, // max numbre of retries -> set the metaRetries
  ): URIO[WalletAccessContext, Int]

  def updateAfterFail(
      recordId: UUID,
      failReason: Option[String],
  ): URIO[WalletAccessContext, Int]

}
