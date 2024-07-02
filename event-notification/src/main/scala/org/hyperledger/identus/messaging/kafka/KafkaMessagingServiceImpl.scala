package org.hyperledger.identus.messaging.kafka

import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import org.hyperledger.identus.messaging.*
import zio.{durationInt, Task, UIO, ULayer, ZIO, ZLayer}
import zio.stream.ZStream

import java.util.Properties
import scala.jdk.CollectionConverters.*

val BOOTSTRAP_SERVERS = "localhost:29092"

class KafkaMessagingServiceImpl extends MessagingService {
  override def makeConsumer[K, V](implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Consumer[K, V]] =
    ZIO.succeed(new KafkaConsumerImpl[K, V](kSerde, vSerde))

  override def makeProducer[K, V](implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Producer[K, V]] =
    ZIO.succeed(new KafkaProducerImpl[K, V](kSerde, vSerde))
}

object KafkaMessagingServiceImpl {
  val layer: ULayer[MessagingService] = ZLayer.succeed(new KafkaMessagingServiceImpl())
}

class KafkaConsumerImpl[K, V](kSerde: Serde[K], vSerde: Serde[V]) extends Consumer[K, V] {
  private val kafkaKeyDeserializer: Deserializer[K] = new Deserializer[K] {
    override def deserialize(topic: String, data: Array[Byte]): K = kSerde.deserialize(data)
  }

  private val kafkaValueDeserializer: Deserializer[V] = new Deserializer[V] {
    override def deserialize(topic: String, data: Array[Byte]): V = vSerde.deserialize(data)
  }

  private val props = new Properties
  props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS)
  props.put(ConsumerConfig.GROUP_ID_CONFIG, "myGroupId")
  private val consumer = new KafkaConsumer(props, kafkaKeyDeserializer, kafkaValueDeserializer)

  override def consume(topic: String, topics: String*)(handler: Message[K, V] => UIO[Unit]): Task[Unit] = {
    ZIO.succeed(consumer.subscribe(List(topic).concat(topics).asJava)) *>
      ZStream
        .repeatZIO(ZIO.attempt(consumer.poll(50.millis)))
        .mapZIO(records => ZIO.foreach(records.asScala)(r => handler(Message(r.key(), r.value(), r.offset()))))
        .runDrain
  }
}

class KafkaProducerImpl[K, V](kSerde: Serde[K], vSerde: Serde[V]) extends Producer[K, V] {

  private val kafkaKeySerializer = new Serializer[K] {
    override def serialize(topic: String, data: K): Array[Byte] = kSerde.serialize(data)
  }
  private val kafkaValueSerializer = new Serializer[V] {
    override def serialize(topic: String, data: V): Array[Byte] = vSerde.serialize(data)
  }

  private val props = new Properties
  props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS)
  val producer = new KafkaProducer(props, kafkaKeySerializer, kafkaValueSerializer)

  override def produce(topic: String, key: K, value: V): Task[Unit] =
    ZIO.logInfo("Producing message...") *>
      ZIO
        .fromFutureJava(producer.send(new ProducerRecord(topic, key, value)))
        .map(_ => ())
}
