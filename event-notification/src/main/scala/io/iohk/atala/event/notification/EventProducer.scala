package io.iohk.atala.event.notification

import zio.IO

trait EventProducer[A]:
  def send(event: Event[A]): IO[EventNotificationServiceError, Unit]
