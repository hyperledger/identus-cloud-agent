package org.hyperledger.identus.shared.messaging.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.header.Headers
import org.hyperledger.identus.shared.messaging.*
import zio.{Duration, RIO, Task, URIO, URLayer, ZIO, ZLayer}
import zio.kafka.consumer.{
  Consumer as ZKConsumer,
  ConsumerSettings as ZKConsumerSettings,
  Subscription as ZKSubscription
}
import zio.kafka.producer.{Producer as ZKProducer, ProducerSettings as ZKProducerSettings}
import zio.kafka.serde.{Deserializer as ZKDeserializer, Serializer as ZKSerializer}

class ZKafkaMessagingServiceImpl(
    bootstrapServers: List[String],
    autoCreateTopics: Boolean,
    maxPollRecords: Int,
    maxPollInterval: Duration,
    pollTimeout: Duration,
    rebalanceSafeCommits: Boolean
) extends MessagingService {
  override def makeConsumer[K, V](groupId: String)(implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Consumer[K, V]] =
    ZIO.succeed(
      new ZKafkaConsumerImpl[K, V](
        bootstrapServers,
        groupId,
        kSerde,
        vSerde,
        autoCreateTopics,
        maxPollRecords,
        maxPollInterval,
        pollTimeout,
        rebalanceSafeCommits
      )
    )

  override def makeProducer[K, V]()(implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Producer[K, V]] =
    ZIO.succeed(new ZKafkaProducerImpl[K, V](bootstrapServers, kSerde, vSerde))
}

object ZKafkaMessagingServiceImpl {
  val layer: URLayer[MessagingServiceConfig, MessagingService] =
    ZLayer.fromZIO {
      for {
        config <- ZIO.service[MessagingServiceConfig]
        kafkaConfig <- config.kafka match
          case Some(cfg) => ZIO.succeed(cfg)
          case None      => ZIO.dieMessage("Kafka config is undefined")
      } yield new ZKafkaMessagingServiceImpl(
        kafkaConfig.bootstrapServers.split(',').toList,
        kafkaConfig.consumers.autoCreateTopics,
        kafkaConfig.consumers.maxPollRecords,
        kafkaConfig.consumers.maxPollInterval,
        kafkaConfig.consumers.pollTimeout,
        kafkaConfig.consumers.rebalanceSafeCommits
      )
    }
}

class ZKafkaConsumerImpl[K, V](
    bootstrapServers: List[String],
    groupId: String,
    kSerde: Serde[K],
    vSerde: Serde[V],
    autoCreateTopics: Boolean,
    maxPollRecords: Int,
    maxPollInterval: Duration,
    pollTimeout: Duration,
    rebalanceSafeCommits: Boolean
) extends Consumer[K, V] {
  private val zkConsumer = ZLayer.scoped(
    ZKConsumer.make(
      ZKConsumerSettings(bootstrapServers)
        .withProperty(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, autoCreateTopics.toString)
        .withGroupId(groupId)
        // 'max.poll.records' default is 500. This is a Kafka property.
        .withMaxPollRecords(maxPollRecords)
        // 'max.poll.interval.ms' default is 5 minutes. This is a Kafka property.
        .withMaxPollInterval(maxPollInterval) // Should be max.poll.records x 'max processing time per record'
        // 'pollTimeout' default is 50 millis. This is a ZIO Kafka property.
        .withPollTimeout(pollTimeout)
        // .withOffsetRetrieval(OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest))
        .withRebalanceSafeCommits(rebalanceSafeCommits)
        // .withMaxRebalanceDuration(30.seconds)
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
      .mapZIO(record =>
        handler(Message(record.key, record.value, record.offset.offset, record.timestamp)).as(record.offset)
      )
      .aggregateAsync(ZKConsumer.offsetBatches)
      .mapZIO(_.commit)
      .runDrain
}

class ZKafkaProducerImpl[K, V](bootstrapServers: List[String], kSerde: Serde[K], vSerde: Serde[V])
    extends Producer[K, V] {
  private val zkProducer = ZLayer.scoped(
    ZKProducer.make(
      ZKProducerSettings(bootstrapServers)
    )
  )

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
