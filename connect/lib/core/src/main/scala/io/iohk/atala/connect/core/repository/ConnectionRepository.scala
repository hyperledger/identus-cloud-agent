package io.iohk.atala.connect.core.repository

import io.iohk.atala.connect.core.model.ConnectionRecord.ProtocolState
import zio.*

import java.util.UUID
import io.iohk.atala.connect.core.model._
trait ConnectionRepository[F[_]] {
  def createConnectionRecord(record: ConnectionRecord): F[Int]

  def getConnectionRecords(): F[Seq[ConnectionRecord]]

  def getConnectionRecord(recordId: UUID): F[Option[ConnectionRecord]]

  def deleteConnectionRecord(recordId: UUID): F[Int]

  def getConnectionRecordByThreadId(thid: UUID): F[Option[ConnectionRecord]]

  def updateWithConnectionRequest(recordId: UUID, request: ConnectionRequest, state: ProtocolState): F[Int]

  def updateWithConnectionResponse(recordId: UUID, request: ConnectionResponse, state: ProtocolState): F[Int]

  def updateConnectionProtocolState(recordId: UUID, from: ProtocolState, to: ProtocolState): F[Int]

}
