package io.iohk.atala.connect.core.service

import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.connect.core.repository.ConnectionRepository
import io.iohk.atala.connect.core.service.ConnectionServiceWithEventNotificationImpl.given
import io.iohk.atala.event.notification.EventNotificationServiceError.EncoderError
import io.iohk.atala.event.notification.{Event, EventEncoder, EventNotificationService}
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.connection.{ConnectionRequest, ConnectionResponse}
import zio.{IO, Task, URLayer, ZIO, ZLayer}

import java.util.UUID

class ConnectionServiceWithEventNotificationImpl(
    connectionRepository: ConnectionRepository[Task],
    eventNotificationService: EventNotificationService
) extends ConnectionServiceImpl(connectionRepository) {
  override def createConnectionInvitation(
      label: Option[String],
      pairwiseDID: DidId
  ): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(super.createConnectionInvitation(label, pairwiseDID))

  override def receiveConnectionInvitation(invitation: String): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(super.receiveConnectionInvitation(invitation))

  override def acceptConnectionInvitation(
      recordId: UUID,
      pairwiseDid: DidId
  ): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(super.acceptConnectionInvitation(recordId, pairwiseDid))

  override def markConnectionRequestSent(recordId: UUID): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(super.markConnectionRequestSent(recordId))

  override def receiveConnectionRequest(request: ConnectionRequest): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(super.receiveConnectionRequest(request))

  override def acceptConnectionRequest(recordId: UUID): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(super.acceptConnectionRequest(recordId))

  override def markConnectionResponseSent(recordId: UUID): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(super.markConnectionResponseSent(recordId))

  override def receiveConnectionResponse(response: ConnectionResponse): IO[ConnectionServiceError, ConnectionRecord] =
    notifyOnSuccess(super.receiveConnectionResponse(response))

  private[this] def notifyOnSuccess(effect: IO[ConnectionServiceError, ConnectionRecord]) =
    for {
      record <- effect
      _ <- notify(record)
    } yield record

  private[this] def notify(record: ConnectionRecord) = {
    val result = for {
      producer <- eventNotificationService.producer[ConnectionRecord]("Connect")
      _ <- producer.send(Event(record))
    } yield ()
    result.catchAll(e => ZIO.logError(s"Notification service error: $e"))
  }
}

object ConnectionServiceWithEventNotificationImpl {
  given EventEncoder[ConnectionRecord] = (data: ConnectionRecord) =>
    ZIO.attempt(data.asInstanceOf[Any]).mapError(t => EncoderError(t.getMessage))

  val layer: URLayer[ConnectionRepository[Task] with EventNotificationService, ConnectionService] =
    ZLayer.fromFunction(ConnectionServiceWithEventNotificationImpl(_, _))
}
