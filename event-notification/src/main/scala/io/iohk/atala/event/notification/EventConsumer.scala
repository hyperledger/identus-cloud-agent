package io.iohk.atala.event.notification

import zio.IO

trait EventConsumer:
  def poll(count: Int): IO[EventConsumer.Error, Seq[Event]]

object EventConsumer:
  sealed trait Error
