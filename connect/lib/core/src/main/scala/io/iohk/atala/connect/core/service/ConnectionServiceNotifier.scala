package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.event.notification.{Event, EventNotificationService}
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.connection.{ConnectionRequest, ConnectionResponse}
import zio.{IO, URLayer, ZIO, ZLayer}

import java.util.UUID

class ConnectionServiceNotifier(
    svc: ConnectionService,
    eventNotificationService: EventNotificationService
) extends ConnectionService {

  private val connectionUpdatedEvent = "ConnectionUpdated"

  override def createConnectionInvitation(
      label: Option[String],
      pairwiseDID: DidId
  ): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(svc.createConnectionInvitation(label, pairwiseDID))

  override def receiveConnectionInvitation(invitation: String): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(svc.receiveConnectionInvitation(invitation))

  override def acceptConnectionInvitation(
      recordId: UUID,
      pairwiseDid: DidId
  ): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(svc.acceptConnectionInvitation(recordId, pairwiseDid))

  override def markConnectionRequestSent(recordId: UUID): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(svc.markConnectionRequestSent(recordId))

  override def receiveConnectionRequest(request: ConnectionRequest): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(svc.receiveConnectionRequest(request))

  override def acceptConnectionRequest(recordId: UUID): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(svc.acceptConnectionRequest(recordId))

  override def markConnectionResponseSent(recordId: UUID): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(svc.markConnectionResponseSent(recordId))

  override def receiveConnectionResponse(response: ConnectionResponse): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(svc.receiveConnectionResponse(response))

  private[this] def notifyOnSuccess(effect: IO[ConnectionServiceError, ConnectionRecord]) =
    for {
      record <- effect
      _ <- notify(record)
    } yield record

  private[this] def notify(record: ConnectionRecord) = {
    val result = for {
      producer <- eventNotificationService.producer[ConnectionRecord]("Connect")
      _ <- producer.send(Event(connectionUpdatedEvent, record))
    } yield ()
    result.catchAll(e => ZIO.logError(s"Notification service error: $e"))
  }

  override def getConnectionRecord(recordId: UUID): IO[ConnectionServiceError, Option[ConnectionRecord]] =
    svc.getConnectionRecord(recordId)

  override def getConnectionRecordByThreadId(thid: String): IO[ConnectionServiceError, Option[ConnectionRecord]] =
    svc.getConnectionRecordByThreadId(thid)

  override def deleteConnectionRecord(recordId: UUID): IO[ConnectionServiceError, Int] =
    svc.deleteConnectionRecord(recordId)

  override def reportProcessingFailure(recordId: UUID, failReason: Option[String]): IO[ConnectionServiceError, Unit] =
    svc.reportProcessingFailure(recordId, failReason)

  override def getConnectionRecords(): IO[ConnectionServiceError, Seq[ConnectionRecord]] =
    svc.getConnectionRecords()

  override def getConnectionRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: ConnectionRecord.ProtocolState*
  ): IO[ConnectionServiceError, Seq[ConnectionRecord]] =
    svc.getConnectionRecordsByStates(ignoreWithZeroRetries, limit, states: _*)
}

object ConnectionServiceNotifier {
  val layer: URLayer[ConnectionService & EventNotificationService, ConnectionService] =
    ZLayer.fromFunction(ConnectionServiceNotifier(_, _))
}
