package org.hyperledger.identus.messaging

import zio.{Cause, Duration, EnvironmentTag, RIO, Task, URIO, ZIO}

import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer
import java.time.Instant
import java.util.UUID

trait MessagingService {
  def makeConsumer[K, V](groupId: String)(implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Consumer[K, V]]
  def makeProducer[K, V]()(implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Producer[K, V]]
}

object MessagingService {

  case class RetryStep(topicName: String, consumerCount: Int, consumerBackoff: Duration, nextTopicName: String)

  def consumeWithRetryStrategy[K: EnvironmentTag, V: EnvironmentTag, HR](
      groupId: String,
      handler: Message[K, V] => RIO[HR, Unit],
      steps: Seq[RetryStep]
  )(implicit kSerde: Serde[K], vSerde: Serde[V]): RIO[HR & Producer[K, V] & MessagingService, Unit] = {

    for {
      messagingService <- ZIO.service[MessagingService]
      messageProducer <- ZIO.service[Producer[K, V]]
      _ <- ZIO.foreachPar(steps) { step =>
        ZIO.foreachPar(1 to step.consumerCount)(_ =>
          for {
            consumer <- messagingService.makeConsumer[K, V](groupId)
            _ <- consumer
              .consume[HR](step.topicName) { m =>
                for {
                  // Wait configured backoff before processing message
                  millisSpentInQueue <- ZIO.succeed(Instant.now().toEpochMilli - m.timestamp)
                  sleepDelay = step.consumerBackoff.toMillis - millisSpentInQueue
                  _ <- ZIO.when(sleepDelay > 0)(ZIO.sleep(Duration.fromMillis(sleepDelay)))
                  _ <- handler(m)
                    .catchAll { t =>
                      for {
                        _ <- ZIO.logErrorCause(s"Connect - Error processing message: ${m.key} ", Cause.fail(t))
                        _ <- messageProducer
                          .produce(step.nextTopicName, m.key, m.value)
                          .catchAll(t => ZIO.logErrorCause("Unable to send message to the next topic", Cause.fail(t)))
                      } yield ()
                    }
                    .catchAllDefect(t =>
                      ZIO.logErrorCause(s"Connect - Defect processing message: ${m.key} ", Cause.fail(t))
                    )
                } yield ()
              }
              .debug
              .fork
          } yield ()
        )
      }
    } yield ()
  }

}

case class Message[K, V](key: K, value: V, offset: Long, timestamp: Long)

case class ByteArrayWrapper(ba: Array[Byte])

trait Consumer[K, V] {
  def consume[HR](topic: String, topics: String*)(handler: Message[K, V] => URIO[HR, Unit]): RIO[HR, Unit]
}
trait Producer[K, V] {
  def produce(topic: String, key: K, value: V): Task[Unit]
}

trait Serde[T] {
  def serialize(t: T): Array[Byte]
  def deserialize(ba: Array[Byte]): T
}

object Serde {
  given byteArraySerde: Serde[ByteArrayWrapper] = new Serde[ByteArrayWrapper] {
    override def serialize(t: ByteArrayWrapper): Array[Byte] = t.ba
    override def deserialize(ba: Array[Byte]): ByteArrayWrapper = ByteArrayWrapper(ba)
  }

  given stringSerde: Serde[String] = new Serde[String] {
    override def serialize(t: String): Array[Byte] = t.getBytes()
    override def deserialize(ba: Array[Byte]): String = new String(ba, StandardCharsets.UTF_8)
  }

  given intSerde: Serde[Int] = new Serde[Int] {
    override def serialize(t: Int): Array[Byte] = {
      val buffer = java.nio.ByteBuffer.allocate(4)
      buffer.putInt(t)
      buffer.array()
    }
    override def deserialize(ba: Array[Byte]): Int = ByteBuffer.wrap(ba).getInt()
  }

  given uuidSerde: Serde[UUID] = new Serde[UUID] {
    override def serialize(t: UUID): Array[Byte] = {
      val buffer = java.nio.ByteBuffer.allocate(16)
      buffer.putLong(t.getMostSignificantBits)
      buffer.putLong(t.getLeastSignificantBits)
      buffer.array()
    }
    override def deserialize(ba: Array[Byte]): UUID = {
      val byteBuffer = ByteBuffer.wrap(ba)
      val high = byteBuffer.getLong
      val low = byteBuffer.getLong
      new UUID(high, low)
    }
  }
}
