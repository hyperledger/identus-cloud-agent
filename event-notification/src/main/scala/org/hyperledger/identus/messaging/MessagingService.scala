package org.hyperledger.identus.messaging

import zio.{Task, UIO}

import java.nio.charset.StandardCharsets
import java.nio.ByteBuffer

trait MessagingService {
  def makeConsumer[K, V](implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Consumer[K, V]]
  def makeProducer[K, V](implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Producer[K, V]]
}

case class Message[K, V](key: K, value: V)

trait Consumer[K, V] {
  def consume(topic: String, topics: String*)(handler: Message[K, V] => UIO[Unit]): Task[Unit]
}
trait Producer[K, V] {
  def produce(topic: String, message: Message[K, V]): Task[Unit]
}

trait Serde[T] {
  def serialize(t: T): Array[Byte]
  def deserialize(ba: Array[Byte]): T
}

object Serde {
  implicit val stringSerde: Serde[String] = new Serde[String] {
    override def serialize(t: String): Array[Byte] = t.getBytes()

    override def deserialize(ba: Array[Byte]): String = new String(ba, StandardCharsets.UTF_8)
  }

  implicit val intSerde: Serde[Int] = new Serde[Int] {
    override def serialize(t: Int): Array[Byte] = {
      val buffer = java.nio.ByteBuffer.allocate(4)
      buffer.putInt(t)
      buffer.array()
    }

    override def deserialize(ba: Array[Byte]): Int = ByteBuffer.wrap(ba).getInt()
  }
}
