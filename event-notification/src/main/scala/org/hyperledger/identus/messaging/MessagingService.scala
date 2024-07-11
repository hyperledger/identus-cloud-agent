package org.hyperledger.identus.messaging

import zio.{RIO, Task, URIO}

import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer
import java.util.UUID

trait MessagingService {
  def makeConsumer[K, V](groupId: String)(implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Consumer[K, V]]
  def makeProducer[K, V]()(implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Producer[K, V]]
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
