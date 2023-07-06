package io.iohk.atala.event.notification
import java.time.Instant
import java.util.UUID
import zio.IO

case class Event[A](id: UUID, ts: Instant, data: A){
  def map[B](f: A => B): Event[B] = copy(data = f(data))
  def mapZIO[E, B](f: A => IO[E, B]): IO[E, Event[B]] = f(data).map(d => copy(data = d))
  def convert[B](implicit conversion: Conversion[A, B]): Event[B] = map(conversion.convert)
}

object Event {
  def apply[A](data: A): Event[A] = Event(UUID.randomUUID(), Instant.now(), data)
}