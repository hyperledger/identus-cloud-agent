package io.iohk.atala.agent.notification

import zio.IO

trait EventConsumer:
  def poll(count: Int): IO[EventConsumer.Error, Seq[Event]]

object EventConsumer:
  sealed trait Error
