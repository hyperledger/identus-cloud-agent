package io.iohk.atala.event.notification

import zio.*
import zio.test.*

object EventNotificationServiceImplSpec extends ZIOSpecDefault {

  private val eventNotificationServiceLayer = ZLayer.succeed(10) >>> EventNotificationServiceImpl.layer

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("EventNotificationServiceImpl")(
      test("should send events between a producer and a consumer of the same topic") {
        for {
          svc <- ZIO.service[EventNotificationService]
          producer <- svc.producer[String]("TopicA")
          consumer <- svc.consumer[String]("TopicA")
          _ <- producer.send(Event("event #1"))
          _ <- producer.send(Event("event #2"))
          events <- consumer.poll(2)
        } yield assertTrue(events == Seq(Event("event #1"), Event("event #2")))
      },
      test("should not mix-up events from different topics") {
        for {
          svc <- ZIO.service[EventNotificationService]
          producerA <- svc.producer[String]("TopicA")
          consumerA <- svc.consumer[String]("TopicA")
          producerB <- svc.producer[String]("TopicB")
          consumerB <- svc.consumer[String]("TopicB")
          _ <- producerA.send(Event("event #1"))
          _ <- producerA.send(Event("event #2"))
          _ <- producerB.send(Event("event #3"))
          eventsA <- consumerA.poll(5)
          eventsB <- consumerB.poll(5)
        } yield {
          assertTrue(eventsA.size == 2) &&
          assertTrue(eventsB.size == 1) &&
          assertTrue(eventsA == Seq(Event("event #1"), Event("event #2"))) &&
          assertTrue(eventsB == Seq(Event("event #3")))
        }
      },
      test("should only deliver the requested messages number to a consumer") {
        for {
          svc <- ZIO.service[EventNotificationService]
          producer <- svc.producer[String]("TopicA")
          consumer <- svc.consumer[String]("TopicA")
          _ <- producer.send(Event("event #1"))
          _ <- producer.send(Event("event #2"))
          events <- consumer.poll(1)
        } yield assertTrue(events == Seq(Event("event #1")))
      },
      test("should remove consumed messages from the queue") {
        for {
          svc <- ZIO.service[EventNotificationService]
          producer <- svc.producer[String]("TopicA")
          consumer <- svc.consumer[String]("TopicA")
          _ <- producer.send(Event("event #1"))
          _ <- producer.send(Event("event #2"))
          _ <- consumer.poll(1)
          events <- consumer.poll(1)
        } yield assertTrue(events == Seq(Event("event #2")))
      },
      test("should send event even when consumer is created and polling first") {
        for {
          svc <- ZIO.service[EventNotificationService]
          // Consuming in a fiber
          consumer <- svc.consumer[String]("TopicA")
          consumerFiber <- consumer.poll(1).fork
          // Producing in another fiber, after 3 seconds
          producer <- svc.producer[String]("TopicA")
          producerFiber <- producer.send(Event("event #1")).delay(3.seconds).fork
          _ <- TestClock.adjust(3.seconds)
          events <- consumerFiber.join
          _ <- producerFiber.join
        } yield assertTrue(events == Seq(Event("event #1")))
      },
      test("should block on sending new messages when queue is full") {
        for {
          svc <- ZIO.service[EventNotificationService]
          producer <- svc.producer[String]("TopicA")
          _ <- ZIO.collectAll((1 to 10).map(i => producer.send(Event(s"event #$i"))))
          fiber <- producer.send(Event("One more event")).timeout(5.seconds).fork
          _ <- TestClock.adjust(5.seconds)
          res <- fiber.join
        } yield assertTrue(res.isEmpty)
      },
      test("should block on reading new messages when queue is empty") {
        for {
          svc <- ZIO.service[EventNotificationService]
          consumer <- svc.consumer[String]("TopicA")
          fiber <- consumer.poll(1).timeout(5.seconds).fork
          _ <- TestClock.adjust(5.seconds)
          res <- fiber.join
        } yield assertTrue(res.isEmpty)
      }
    )
  }.provideLayer(eventNotificationServiceLayer)

}
