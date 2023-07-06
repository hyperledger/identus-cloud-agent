package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.connection.{ConnectionRequest, ConnectionResponse}
import zio.mock.{Mock, Proxy}
import zio.{IO, URLayer, ZIO, ZLayer, mock}

import java.util.UUID

object MockConnectionService extends Mock[ConnectionService] {

  object CreateConnectionInvitation extends Effect[(Option[String], DidId), ConnectionServiceError, ConnectionRecord]
  object ReceiveConnectionInvitation extends Effect[String, ConnectionServiceError, ConnectionRecord]
  object AcceptConnectionInvitation extends Effect[(UUID, DidId), ConnectionServiceError, ConnectionRecord]
  object MarkConnectionRequestSent extends Effect[UUID, ConnectionServiceError, ConnectionRecord]
  object ReceiveConnectionRequest extends Effect[ConnectionRequest, ConnectionServiceError, ConnectionRecord]
  object AcceptConnectionRequest extends Effect[UUID, ConnectionServiceError, ConnectionRecord]
  object MarkConnectionResponseSent extends Effect[UUID, ConnectionServiceError, ConnectionRecord]
  object ReceiveConnectionResponse extends Effect[ConnectionResponse, ConnectionServiceError, ConnectionRecord]

  override val compose: URLayer[mock.Proxy, ConnectionService] = ZLayer {
    for {
      proxy <- ZIO.service[Proxy]
    } yield new ConnectionService {
      override def createConnectionInvitation(
          label: Option[String],
          pairwiseDID: DidId
      ): IO[ConnectionServiceError, ConnectionRecord] =
        proxy(CreateConnectionInvitation, label, pairwiseDID)

      override def receiveConnectionInvitation(invitation: String): IO[ConnectionServiceError, ConnectionRecord] =
        proxy(ReceiveConnectionInvitation, invitation)

      override def acceptConnectionInvitation(
          recordId: UUID,
          pairwiseDid: DidId
      ): IO[ConnectionServiceError, ConnectionRecord] =
        proxy(AcceptConnectionInvitation, recordId, pairwiseDid)

      override def markConnectionRequestSent(recordId: UUID): IO[ConnectionServiceError, ConnectionRecord] =
        proxy(MarkConnectionRequestSent, recordId)

      override def receiveConnectionRequest(request: ConnectionRequest): IO[ConnectionServiceError, ConnectionRecord] =
        proxy(ReceiveConnectionRequest, request)

      override def acceptConnectionRequest(recordId: UUID): IO[ConnectionServiceError, ConnectionRecord] =
        proxy(AcceptConnectionRequest, recordId)

      override def markConnectionResponseSent(recordId: UUID): IO[ConnectionServiceError, ConnectionRecord] =
        proxy(MarkConnectionResponseSent, recordId)

      override def receiveConnectionResponse(
          response: ConnectionResponse
      ): IO[ConnectionServiceError, ConnectionRecord] =
        proxy(ReceiveConnectionResponse, response)

      override def getConnectionRecords(): IO[ConnectionServiceError, Seq[ConnectionRecord]] = ???

      override def getConnectionRecordsByStates(
          ignoreWithZeroRetries: Boolean,
          limit: Int,
          states: ConnectionRecord.ProtocolState*
      ): IO[ConnectionServiceError, Seq[ConnectionRecord]] = ???

      override def getConnectionRecord(recordId: UUID): IO[ConnectionServiceError, Option[ConnectionRecord]] = ???

      override def getConnectionRecordByThreadId(thid: String): IO[ConnectionServiceError, Option[ConnectionRecord]] =
        ???

      override def deleteConnectionRecord(recordId: UUID): IO[ConnectionServiceError, Int] = ???

      override def reportProcessingFailure(
          recordId: UUID,
          failReason: Option[String]
      ): IO[ConnectionServiceError, Unit] = ???
    }
  }
}
