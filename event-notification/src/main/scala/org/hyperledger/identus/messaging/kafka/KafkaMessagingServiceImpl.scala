package org.hyperledger.identus.messaging.kafka

import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.hyperledger.identus.messaging.*
import zio.{durationInt, RIO, Task, ULayer, URIO, ZIO, ZLayer}
import zio.stream.ZStream

import java.util.Properties
import scala.jdk.CollectionConverters.*

class KafkaMessagingServiceImpl(
    bootstrapServers: List[String]
) extends MessagingService {
  override def makeConsumer[K, V](groupId: String)(implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Consumer[K, V]] =
    ZIO.succeed(new KafkaConsumerImpl[K, V](bootstrapServers, groupId, kSerde, vSerde))

  override def makeProducer[K, V]()(implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Producer[K, V]] =
    ZIO.succeed(new KafkaProducerImpl[K, V](bootstrapServers, kSerde, vSerde))
}

object KafkaMessagingServiceImpl {
  def layer(bootstrapServers: List[String]): ULayer[MessagingService] =
    ZLayer.succeed(new KafkaMessagingServiceImpl(bootstrapServers))
}

class KafkaConsumerImpl[K, V](
    bootstrapServers: List[String],
    groupId: String,
    kSerde: Serde[K],
    vSerde: Serde[V]
) extends Consumer[K, V] {
  private val kafkaKeyDeserializer: Deserializer[K] = new Deserializer[K] {
    override def deserialize(topic: String, data: Array[Byte]): K = kSerde.deserialize(data)
  }

  private val kafkaValueDeserializer: Deserializer[V] = new Deserializer[V] {
    override def deserialize(topic: String, data: Array[Byte]): V = vSerde.deserialize(data)
  }

  private val props = new Properties
  props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers.mkString(","))
  props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
  private val consumer = new KafkaConsumer(props, kafkaKeyDeserializer, kafkaValueDeserializer)

  override def consume[HR](topic: String, topics: String*)(handler: Message[K, V] => URIO[HR, Unit]): RIO[HR, Unit] = {
    ZIO.succeed(consumer.subscribe(List(topic).concat(topics).asJava)) *>
      ZStream
        .repeatZIO(ZIO.attempt(consumer.poll(50.millis)))
        .mapZIO(records => ZIO.foreach(records.asScala)(r => handler(Message(r.key(), r.value(), r.offset()))))
        .runDrain
  }
}

class KafkaProducerImpl[K, V](
    bootstrapServers: List[String],
    kSerde: Serde[K],
    vSerde: Serde[V]
) extends Producer[K, V] {

  private val kafkaKeySerializer = new Serializer[K] {
    override def serialize(topic: String, data: K): Array[Byte] = kSerde.serialize(data)
  }
  private val kafkaValueSerializer = new Serializer[V] {
    override def serialize(topic: String, data: V): Array[Byte] = vSerde.serialize(data)
  }

  private val props = new Properties
  props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers.mkString(","))
  val producer = new KafkaProducer(props, kafkaKeySerializer, kafkaValueSerializer)

  override def produce(topic: String, key: K, value: V): Task[Unit] =
    ZIO.logInfo("Producing message...") *>
      ZIO
        .fromFutureJava(producer.send(new ProducerRecord(topic, key, value)))
        .map(_ => ())
}
