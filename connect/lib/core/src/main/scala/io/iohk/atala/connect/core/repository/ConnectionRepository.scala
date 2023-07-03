package io.iohk.atala.connect.core.repository

import io.iohk.atala.connect.core.model.ConnectionRecord.ProtocolState

import java.util.UUID
import io.iohk.atala.mercury.protocol.connection.*
import io.iohk.atala.connect.core.model.ConnectionRecord

trait ConnectionRepository[F[_]] {
  def createConnectionRecord(record: ConnectionRecord): F[Int]

  def getConnectionRecords: F[Seq[ConnectionRecord]]

  def getConnectionRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: ConnectionRecord.ProtocolState*
  ): F[Seq[ConnectionRecord]]

  def getConnectionRecord(recordId: UUID): F[Option[ConnectionRecord]]

  def deleteConnectionRecord(recordId: UUID): F[Int]

  def getConnectionRecordByThreadId(thid: UUID): F[Option[ConnectionRecord]]

  def updateWithConnectionRequest(
      recordId: UUID,
      request: ConnectionRequest,
      state: ProtocolState,
      maxRetries: Int, // max numbre of retries -> set the metaRetries
  ): F[Int]

  def updateWithConnectionResponse(
      recordId: UUID,
      response: ConnectionResponse,
      state: ProtocolState,
      maxRetries: Int, // max numbre of retries -> set the metaRetries
  ): F[Int]

  def updateConnectionProtocolState(
      recordId: UUID,
      from: ProtocolState,
      to: ProtocolState,
      maxRetries: Int, // max numbre of retries -> set the metaRetries
  ): F[Int]

  def updateAfterFail(
      recordId: UUID,
      failReason: Option[String],
  ): F[Int]
}
