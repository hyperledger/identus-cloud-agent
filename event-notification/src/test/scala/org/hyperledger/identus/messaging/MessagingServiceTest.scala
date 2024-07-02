package org.hyperledger.identus.messaging

import org.hyperledger.identus.messaging.kafka.KafkaMessagingServiceImpl
import zio.{durationInt, Schedule, Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

import scala.util.Random

object MessagingServiceTest extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val effect = for {
      ms <- ZIO.service[MessagingService]
      consumer <- ms.makeConsumer[String, Int]
      producer <- ms.makeProducer[String, Int]
      f1 <- consumer.consume("Connect")(message => ZIO.logInfo(s"Message => $message")).fork
      f2 <- producer.produce("Connect", Message("key", Random.nextInt().abs)).repeat(Schedule.spaced(1.second)).fork
      _ <- f1.join
      _ <- ZIO.never
    } yield ()
    effect.provide(KafkaMessagingServiceImpl.layer)
  }
}
