package io.iohk.atala.event.notification

import io.iohk.atala.event.notification.EventNotificationServiceError.{DecoderError, EncoderError}
import zio.IO

trait EventNotificationService:
  def consumer[A](topic: String): IO[EventNotificationServiceError, EventConsumer[A]]
  def producer[A](topic: String): IO[EventNotificationServiceError, EventProducer[A]]
