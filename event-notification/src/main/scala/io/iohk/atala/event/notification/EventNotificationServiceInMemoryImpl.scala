package io.iohk.atala.event.notification

import io.iohk.atala.event.notification.EventNotificationServiceError.EventSendingFailed
import zio.{IO, Queue, ULayer, URLayer, ZIO, ZLayer}

import scala.collection.mutable

class EventNotificationServiceInMemoryImpl extends EventNotificationService:
  private[this] val queueMap = mutable.Map.empty[String, Queue[Event[Any]]]

  private[this] def getOrCreateQueue(topic: String): IO[EventNotificationServiceError, Queue[Event[Any]]] = {
    for {
      maybeQueue <- ZIO.succeed(queueMap.get(topic))
      queue <- maybeQueue match
        case Some(value) => ZIO.succeed(value)
        case None        => Queue.bounded(500)
      _ <- ZIO.succeed(queueMap.put(topic, queue))
    } yield queue
  }

  override def consumer[A](
      topic: String
  )(using decoder: EventDecoder[A]): IO[EventNotificationServiceError, EventConsumer[A]] =
    ZIO.succeed(new EventConsumer[A] {
      override def poll(count: Int): IO[EventNotificationServiceError, Seq[Event[A]]] = for {
        queue <- getOrCreateQueue(topic)
        events <- queue.takeBetween(1, count)
        decodedEvents <- ZIO.foreach(events)(e => decoder.decode(e.data).map(d => Event(d)))
      } yield decodedEvents
    })

  override def producer[A](
      topic: String
  )(using encoder: EventEncoder[A]): IO[EventNotificationServiceError, EventProducer[A]] =
    ZIO.succeed(new EventProducer[A] {
      override def send(event: Event[A]): IO[EventNotificationServiceError, Unit] = for {
        queue <- getOrCreateQueue(topic)
        encodedEvent <- encoder.encode(event.data).map(e => Event(e))
        succeeded <- queue.offer(encodedEvent)
        _ <- if (succeeded) ZIO.unit else ZIO.fail(EventSendingFailed("Queue.offer returned 'false'"))
      } yield ()
    })

object EventNotificationServiceInMemoryImpl {
  val layer: ULayer[EventNotificationServiceInMemoryImpl] =
    ZLayer.succeed(new EventNotificationServiceInMemoryImpl())
}
