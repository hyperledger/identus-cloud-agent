package io.iohk.atala.agent.notification

import zio.IO

trait EventNotificationService:
  def notify(event: Event): IO[EventNotificationService.Error, Unit]
  def subscribe(topic: String): IO[EventNotificationService.Error, EventConsumer]

object EventNotificationService:
  sealed trait Error
