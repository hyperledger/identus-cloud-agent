package io.iohk.atala.event.notification

import io.iohk.atala.event.notification.EventNotificationServiceError.{DecoderError, EncoderError}
import zio.IO

trait EventNotificationService:
  def consumer[A](topic: String)(using decoder: EventDecoder[A]): IO[EventNotificationServiceError, EventConsumer[A]]
  def producer[A](topic: String)(using encoder: EventEncoder[A]): IO[EventNotificationServiceError, EventProducer[A]]

trait EventEncoder[A]:
  def encode(data: A): IO[EncoderError, Any]

trait EventDecoder[A]:
  def decode(data: Any): IO[DecoderError, A]
