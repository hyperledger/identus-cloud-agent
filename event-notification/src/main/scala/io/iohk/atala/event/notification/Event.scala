package io.iohk.atala.event.notification
import java.time.Instant
import java.util.UUID
import zio.IO

case class Event[A](`type`: String, id: UUID, ts: Instant, data: A)

object Event {
  def apply[A](`type`: String, data: A): Event[A] = Event(`type`, UUID.randomUUID(), Instant.now(), data)
}
