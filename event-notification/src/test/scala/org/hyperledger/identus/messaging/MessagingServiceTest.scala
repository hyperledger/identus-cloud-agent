package org.hyperledger.identus.messaging

import org.hyperledger.identus.messaging.kafka.ZKafkaMessagingServiceImpl
import zio.{durationInt, Random, Schedule, Scope, ZIO, ZIOAppArgs, ZIOAppDefault}
import zio.json.{DecoderOps, DeriveJsonDecoder, DeriveJsonEncoder, EncoderOps, JsonDecoder, JsonEncoder}

import java.nio.charset.StandardCharsets
import java.util.UUID

case class Customer(name: String)

object Customer {
  given encoder: JsonEncoder[Customer] = DeriveJsonEncoder.gen[Customer]
  given decoder: JsonDecoder[Customer] = DeriveJsonDecoder.gen[Customer]
  given serde: Serde[Customer] = new Serde[Customer]:
    override def serialize(t: Customer): Array[Byte] =
      t.toJson.getBytes(StandardCharsets.UTF_8)
    override def deserialize(ba: Array[Byte]): Customer =
      new String(ba, StandardCharsets.UTF_8).fromJson[Customer].getOrElse(Customer("Parsing Error"))
}

object MessagingServiceTest extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val effect = for {
      ms <- ZIO.service[MessagingService]
      consumer <- ms.makeConsumer[UUID, Customer]
      producer <- ms.makeProducer[UUID, Customer]
      f1 <- consumer
        .consume("Connect")(msg => ZIO.logInfo(s"Handling new message: ${msg.offset} - ${msg.key} - ${msg.value}"))
        .fork
      f2 <- Random.nextUUID
        .flatMap(uuid => producer.produce("Connect", uuid, Customer(s"Name $uuid")))
        .repeat(Schedule.spaced(500.millis))
        .fork
      _ <- ZIO.never
    } yield ()
    effect.provide(ZKafkaMessagingServiceImpl.layer)
  }
}
