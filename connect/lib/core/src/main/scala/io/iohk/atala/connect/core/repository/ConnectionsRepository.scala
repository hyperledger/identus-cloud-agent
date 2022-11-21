package io.iohk.atala.connect.core.repository

import io.iohk.atala.connect.core.model.ConnectionRecord.ProtocolState
import io.iohk.atala.connect.core.model.ConnectionRecord.ProtocolState.{ConnectionRequestPending, ConnectionRequestSent}
import zio.*

import java.util.UUID
import io.iohk.atala.mercury.protocol.invitation
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation
import io.iohk.atala.mercury.protocol.connection.*
import io.iohk.atala.connect.core.model.ConnectionRecord

trait ConnectionsRepository[F[_]] {
  def createConnectionRecord(record: ConnectionRecord): F[Int]

  def getConnectionRecords: F[Seq[ConnectionRecord]]

  def getConnectionRecord(id: UUID): F[Option[ConnectionRecord]]

  def deleteConnectionRecord(id: UUID): F[Int]

  def deleteConnectionRecordByThreadId(id: UUID): F[Int]

  def getConnectionRecordByThreadId(id: UUID): F[Option[ConnectionRecord]]

  def updateWithConnectionRequest(request: ConnectionRequest): F[Int]

  def updateWithConnectionResponse(request: ConnectionResponse): F[Int]

  def updateConnectionProtocolState(
      id: UUID,
      from: ConnectionRecord.ProtocolState,
      to: ConnectionRecord.ProtocolState
  ): F[Int]

}
