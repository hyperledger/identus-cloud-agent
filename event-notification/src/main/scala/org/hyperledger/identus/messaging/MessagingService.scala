package org.hyperledger.identus.messaging

import org.hyperledger.identus.shared.models.Serde
import zio.{Cause, Duration, EnvironmentTag, RIO, Task, URIO, ZIO}

import java.time.Instant
trait MessagingService {
  def makeConsumer[K, V](groupId: String)(implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Consumer[K, V]]
  def makeProducer[K, V]()(implicit kSerde: Serde[K], vSerde: Serde[V]): Task[Producer[K, V]]
}

object MessagingService {

  case class RetryStep(topicName: String, consumerCount: Int, consumerBackoff: Duration, nextTopicName: String)

  def consumeWithRetryStrategy[K: EnvironmentTag, V: EnvironmentTag, HR](
      groupId: String,
      handler: Message[K, V] => RIO[HR, Unit],
      steps: Seq[RetryStep]
  )(implicit kSerde: Serde[K], vSerde: Serde[V]): RIO[HR & Producer[K, V] & MessagingService, Unit] = {
    for {
      messagingService <- ZIO.service[MessagingService]
      messageProducer <- ZIO.service[Producer[K, V]]
      _ <- ZIO.foreachPar(steps) { step =>
        ZIO.foreachPar(1 to step.consumerCount)(_ =>
          for {
            consumer <- messagingService.makeConsumer[K, V](groupId)
            _ <- consumer
              .consume[HR](step.topicName) { m =>
                for {
                  // Wait configured backoff before processing message
                  millisSpentInQueue <- ZIO.succeed(Instant.now().toEpochMilli - m.timestamp)
                  sleepDelay = step.consumerBackoff.toMillis - millisSpentInQueue
                  _ <- ZIO.when(sleepDelay > 0)(ZIO.sleep(Duration.fromMillis(sleepDelay)))
                  _ <- handler(m)
                    .catchAll { t =>
                      for {
                        _ <- ZIO.logErrorCause(s"Error processing message: ${m.key} ", Cause.fail(t))
                        _ <- messageProducer
                          .produce(step.nextTopicName, m.key, m.value)
                          .catchAll(t => ZIO.logErrorCause("Unable to send message to the next topic", Cause.fail(t)))
                      } yield ()
                    }
                    .catchAllDefect(t =>
                      ZIO.logErrorCause(s"Defect processing message: ${m.key} ", Cause.fail(t))
                    )
                } yield ()
              }
              .debug
              .fork
          } yield ()
        )
      }
    } yield ()
  }

}

case class Message[K, V](key: K, value: V, offset: Long, timestamp: Long)

trait Consumer[K, V] {
  def consume[HR](topic: String, topics: String*)(handler: Message[K, V] => URIO[HR, Unit]): RIO[HR, Unit]
}
trait Producer[K, V] {
  def produce(topic: String, key: K, value: V): Task[Unit]
}
