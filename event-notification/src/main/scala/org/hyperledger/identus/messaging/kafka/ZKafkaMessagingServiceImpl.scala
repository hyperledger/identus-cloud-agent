package org.hyperledger.identus.messaging.kafka

import org.apache.kafka.common.header.Headers
import org.hyperledger.identus.messaging.*
import zio.{RIO, Task, UIO, ULayer, ZIO, ZLayer}
import zio.kafka.consumer.{
  Consumer as ZKConsumer,
  ConsumerSettings as ZKConsumerSettings,
  Subscription as ZKSubscription
}
import zio.kafka.producer.{Producer as ZKProducer, ProducerSettings as ZKProducerSettings}
import zio.kafka.serde.{Deserializer as ZKDeserializer, Serializer as ZKSerializer}

class ZKafkaMessagingServiceImpl extends MessagingService {
  override def makeConsumer[K, V](implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Consumer[K, V]] =
    ZIO.succeed(new ZKafkaConsumerImpl[K, V](kSerde, vSerde))

  override def makeProducer[K, V](implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Producer[K, V]] =
    ZIO.succeed(new ZKafkaProducerImpl[K, V](kSerde, vSerde))
}

object ZKafkaMessagingServiceImpl {
  val layer: ULayer[MessagingService] = ZLayer.succeed(new ZKafkaMessagingServiceImpl())
}

class ZKafkaConsumerImpl[K, V](kSerde: Serde[K], vSerde: Serde[V]) extends Consumer[K, V] {
  private val BOOTSTRAP_SERVERS = List("localhost:29092")
  private val GROUP_ID = "identus-cloud-agent"
  private val zkConsumer = ZLayer.scoped(ZKConsumer.make(ZKConsumerSettings(BOOTSTRAP_SERVERS).withGroupId(GROUP_ID)))

  private val zkKeyDeserializer = new ZKDeserializer[Any, K] {
    override def deserialize(topic: String, headers: Headers, data: Array[Byte]): RIO[Any, K] =
      ZIO.succeed(kSerde.deserialize(data))
  }

  private val zkValueDeserializer = new ZKDeserializer[Any, V] {
    override def deserialize(topic: String, headers: Headers, data: Array[Byte]): RIO[Any, V] =
      ZIO.succeed(vSerde.deserialize(data))
  }

  override def consume(topic: String, topics: String*)(handler: Message[K, V] => UIO[Unit]): Task[Unit] =
    ZKConsumer
      .plainStream(ZKSubscription.topics(topic, topics*), zkKeyDeserializer, zkValueDeserializer)
      .mapZIO(record => handler(Message(record.key, record.value, record.offset.offset)).as(record.offset))
      .aggregateAsync(ZKConsumer.offsetBatches)
      .mapZIO(_.commit)
      .runDrain
      .provideSome(zkConsumer)
}

class ZKafkaProducerImpl[K, V](kSerde: Serde[K], vSerde: Serde[V]) extends Producer[K, V] {
  private val BOOTSTRAP_SERVERS = List("localhost:29092")
  private val zkProducer = ZLayer.scoped(ZKProducer.make(ZKProducerSettings(BOOTSTRAP_SERVERS)))

  private val zkKeySerializer = new ZKSerializer[Any, K] {
    override def serialize(topic: String, headers: Headers, value: K): RIO[Any, Array[Byte]] =
      ZIO.succeed(kSerde.serialize(value))
  }

  private val zkValueSerializer = new ZKSerializer[Any, V] {
    override def serialize(topic: String, headers: Headers, value: V): RIO[Any, Array[Byte]] =
      ZIO.succeed(vSerde.serialize(value))
  }

  override def produce(topic: String, key: K, value: V): Task[Unit] =
    ZKProducer
      .produce(topic, key, value, zkKeySerializer, zkValueSerializer)
      .tap(metadata => ZIO.logInfo(s"Message produced: ${metadata.offset()}"))
      .map(_ => ())
      .provideSome(zkProducer)

}
