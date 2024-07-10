package org.hyperledger.identus.messaging.kafka

import org.apache.kafka.common.header.Headers
import org.hyperledger.identus.messaging.*
import zio.{durationInt, EnvironmentTag, RIO, RLayer, Task, ULayer, URIO, ZIO, ZLayer}
import zio.kafka.consumer.{
  Consumer as ZKConsumer,
  ConsumerSettings as ZKConsumerSettings,
  Subscription as ZKSubscription
}
import zio.kafka.producer.{Producer as ZKProducer, ProducerSettings as ZKProducerSettings}
import zio.kafka.serde.{Deserializer as ZKDeserializer, Serializer as ZKSerializer}

class ZKafkaMessagingServiceImpl(bootstrapServers: List[String]) extends MessagingService {
  override def makeConsumer[K, V](groupId: String)(implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Consumer[K, V]] =
    ZIO.succeed(new ZKafkaConsumerImpl[K, V](bootstrapServers, groupId, kSerde, vSerde))

  override def makeProducer[K, V]()(implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Producer[K, V]] =
    ZIO.succeed(new ZKafkaProducerImpl[K, V](bootstrapServers, kSerde, vSerde))
}

object ZKafkaMessagingServiceImpl {
  def layer(bootstrapServers: List[String]): ULayer[MessagingService] =
    ZLayer.succeed(new ZKafkaMessagingServiceImpl(bootstrapServers))
}

class ZKafkaConsumerImpl[K, V](
    bootstrapServers: List[String],
    groupId: String,
    kSerde: Serde[K],
    vSerde: Serde[V]
) extends Consumer[K, V] {
  private val zkConsumer = ZLayer.scoped(
    ZKConsumer.make(
      ZKConsumerSettings(bootstrapServers)
        .withGroupId(groupId)
        // 'max.poll.records' default is 500. This is a Kafka property.
        .withMaxPollRecords(500)
        // 'max.poll.interval.ms' default is 5 minutes. This is a Kafka property.
        .withMaxPollInterval(5.minutes) // Should be max.poll.records x 'max processing time per record'
        // 'pollTimeout' default is 50 millis. This is a ZIO Kafka property.
        .withPollTimeout(50.millis)
//        .withOffsetRetrieval(OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest))
    )
  )

  private val zkKeyDeserializer = new ZKDeserializer[Any, K] {
    override def deserialize(topic: String, headers: Headers, data: Array[Byte]): RIO[Any, K] =
      ZIO.succeed(kSerde.deserialize(data))
  }

  private val zkValueDeserializer = new ZKDeserializer[Any, V] {
    override def deserialize(topic: String, headers: Headers, data: Array[Byte]): RIO[Any, V] =
      ZIO.succeed(vSerde.deserialize(data))
  }

  override def consume[HR](topic: String, topics: String*)(handler: Message[K, V] => URIO[HR, Unit]): RIO[HR, Unit] =
    ZKConsumer
      .plainStream(ZKSubscription.topics(topic, topics*), zkKeyDeserializer, zkValueDeserializer)
      .provideSomeLayer(zkConsumer)
      .mapZIO(record => handler(Message(record.key, record.value, record.offset.offset)).as(record.offset))
      .aggregateAsync(ZKConsumer.offsetBatches)
      .mapZIO(_.commit)
      .runDrain
}

object ZKafkaConsumerImpl {
  def layer[K: EnvironmentTag, V: EnvironmentTag](groupId: String)(implicit
      kSerde: Serde[K],
      vSerde: Serde[V]
  ): RLayer[MessagingService, Consumer[K, V]] = ZLayer.fromZIO(for {
    messagingService <- ZIO.service[MessagingService]
    consumer <- messagingService.makeConsumer[K, V](groupId)
  } yield consumer)
}

class ZKafkaProducerImpl[K, V](bootstrapServers: List[String], kSerde: Serde[K], vSerde: Serde[V])
    extends Producer[K, V] {
  private val zkProducer = ZLayer.scoped(ZKProducer.make(ZKProducerSettings(bootstrapServers)))

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

object ZKafkaProducerImpl {
  def layer[K: EnvironmentTag, V: EnvironmentTag](implicit
      kSerde: Serde[K],
      vSerde: Serde[V]
  ): RLayer[MessagingService, Producer[K, V]] = ZLayer.fromZIO(for {
    messagingService <- ZIO.service[MessagingService]
    producer <- messagingService.makeProducer[K, V]()
  } yield producer)
}
