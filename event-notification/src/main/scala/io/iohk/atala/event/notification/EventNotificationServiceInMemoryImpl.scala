package io.iohk.atala.event.notification

import zio.{IO, Queue, ULayer, URLayer, ZIO, ZLayer}

class EventNotificationServiceInMemoryImpl(queue: Queue[Event]) extends EventNotificationService {

  override def notify(event: Event): IO[EventNotificationService.Error, Unit] =
    queue.offer(event).unit

  override def subscribe(topic: String): IO[EventNotificationService.Error, EventConsumer] =
    ZIO.succeed(new EventConsumer {
      override def poll(count: Int): IO[EventConsumer.Error, Seq[Event]] = queue.takeBetween(1, count)
    })
}

object EventNotificationServiceInMemoryImpl {
  val layer: URLayer[Queue[Event], EventNotificationServiceInMemoryImpl] =
    ZLayer.fromFunction(new EventNotificationServiceInMemoryImpl(_))
}
