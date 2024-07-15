package org.hyperledger.identus.messaging.kafka

import org.hyperledger.identus.messaging._
import zio._
import zio.concurrent.ConcurrentMap
import zio.stream._
import zio.Clock
import zio.Task
import InMemoryMessagingService.*

import java.util.concurrent.TimeUnit

class InMemoryMessagingService(
    queueMap: ConcurrentMap[String, (Queue[Message[_, _]], Ref[Offset])],
    queueCapacity: Int,
    consumerGroups: ConcurrentMap[String, ConcurrentMap[Offset, TimeStamp]] // Track processed messages by groupId
) extends MessagingService {

  override def makeConsumer[K, V](groupId: String)(using kSerde: Serde[K], vSerde: Serde[V]): Task[Consumer[K, V]] = {
    for {
      processedMessages <- consumerGroups.get(groupId).flatMap {
        case Some(map) => ZIO.succeed(map)
        case None =>
          for {
            newMap <- ConcurrentMap.empty[Offset, TimeStamp]
            _ <- consumerGroups.put(groupId, newMap)
          } yield newMap
      }
    } yield new InMemoryConsumer[K, V](groupId, queueMap, processedMessages)
  }

  override def makeProducer[K, V]()(using kSerde: Serde[K], vSerde: Serde[V]): Task[Producer[K, V]] =
    ZIO.succeed(new InMemoryProducer[K, V](queueMap, queueCapacity))
}

class InMemoryConsumer[K, V](
    groupId: String,
    queueMap: ConcurrentMap[String, (Queue[Message[_, _]], Ref[Offset])],
    processedMessages: ConcurrentMap[Offset, TimeStamp]
) extends Consumer[K, V] {
  override def consume[HR](topic: String, topics: String*)(handler: Message[K, V] => URIO[HR, Unit]): RIO[HR, Unit] = {
    val allTopics = topic +: topics

    def getQueueStream(topic: String): ZStream[Any, Nothing, Message[K, V]] =
      ZStream.repeatZIO {
        queueMap.get(topic).flatMap {
          case Some((queue, _)) =>
            ZIO.debug(s"Connected to queue for topic $topic in group $groupId") *>
              ZIO.succeed(ZStream.fromQueue(queue).collect { case msg: Message[K, V] => msg })
          case None =>
            ZIO.debug(s"Waiting to connect to queue for topic $topic in group $groupId, retrying...") *>
              ZIO.sleep(1.second) *> ZIO.succeed(ZStream.empty)
        }
      }.flatten

    val streams = allTopics.map(getQueueStream)
    ZStream
      .mergeAllUnbounded()(streams: _*)
      .tap(msg => ZIO.log(s"Processing message in group $groupId: $msg"))
      .filterZIO { msg =>
        for {
          currentTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
          isNew <- processedMessages
            .putIfAbsent(Offset(msg.offset), TimeStamp(currentTime))
            .map(_.isEmpty) // Store the current timestamp
        } yield isNew
      } // Ensures message is processed only once
      .mapZIO(handler)
      .runDrain
  }
}

class InMemoryProducer[K, V](
    queueMap: ConcurrentMap[String, (Queue[Message[_, _]], Ref[Offset])],
    queueCapacity: Int
) extends Producer[K, V] {
  override def produce(topic: String, key: K, value: V): Task[Unit] = for {
    queueAndIdRef <- queueMap.get(topic).flatMap {
      case Some(qAndIdRef) => ZIO.succeed(qAndIdRef)
      case None =>
        for {
          newQueue <- Queue.sliding[Message[_, _]](queueCapacity)
          newIdRef <- Ref.make(Offset(0L))
          _ <- queueMap.put(topic, (newQueue, newIdRef))
        } yield (newQueue, newIdRef)
    }
    (queue, idRef) = queueAndIdRef
    currentTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
    messageId <- idRef.updateAndGet(x => Offset(x.value + 1)) //  unique atomic id incremented per topic
    _ <- queue.offer(Message(key, value, messageId.value, currentTime))
    _ <- ZIO.debug(s"Message offered to queue: $topic with ID: $messageId")
  } yield ()
}

object InMemoryMessagingService {

  opaque type Offset = Long
  object Offset:
    def apply(value: Long): Offset = value
    extension (id: Offset) def value: Long = id

  opaque type TimeStamp = Long
  object TimeStamp:
    def apply(value: Long): TimeStamp = value
    extension (ts: TimeStamp) def value: Long = ts

  val messagingServiceLayer: ULayer[MessagingService] =
    ZLayer.fromZIO {
      for {
        queueMap <- ConcurrentMap.empty[String, (Queue[Message[_, _]], Ref[Offset])]
        consumerGroups <- ConcurrentMap.empty[String, ConcurrentMap[Offset, TimeStamp]]
        queueCapacity = 100 // queue capacity
        _ <- cleanupTaskForProcessedMessages(consumerGroups)
      } yield new InMemoryMessagingService(queueMap, queueCapacity, consumerGroups)
    }

  def producerLayer[K: EnvironmentTag, V: EnvironmentTag](using
      kSerde: Serde[K],
      vSerde: Serde[V]
  ): RLayer[MessagingService, Producer[K, V]] =
    ZLayer.fromZIO {
      for {
        messagingService <- ZIO.service[MessagingService]
        producer <- messagingService.makeProducer[K, V]()
      } yield producer
    }

  def consumerLayer[K: EnvironmentTag, V: EnvironmentTag](
      groupId: String
  )(using kSerde: Serde[K], vSerde: Serde[V]): RLayer[MessagingService, Consumer[K, V]] =
    ZLayer.fromZIO {
      for {
        messagingService <- ZIO.service[MessagingService]
        consumer <- messagingService.makeConsumer[K, V](groupId)
      } yield consumer
    }

  private def cleanupTaskForProcessedMessages(
      consumerGroups: ConcurrentMap[String, ConcurrentMap[Offset, TimeStamp]],
      maxAge: Duration = 60.minutes // Maximum age for entries
  ): UIO[Unit] = {
    def cleanupOldEntries(map: ConcurrentMap[Offset, TimeStamp]): UIO[Unit] = for {
      currentTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      entries <- map.toList
      _ <- ZIO.foreachDiscard(entries) { case (key, timestamp) =>
        if (currentTime - timestamp > maxAge.toMillis)
          map.remove(key) *> ZIO.log(s"Removed old entry with key: $key and timestamp: $timestamp")
        else
          ZIO.unit
      }
    } yield ()

    (for {
      entries <- consumerGroups.toList
      _ <- ZIO.foreachDiscard(entries) { case (groupId, map) =>
        ZIO.log(s"Cleaning up entries for group: $groupId") *> cleanupOldEntries(map)
      }
    } yield ())
      .repeat(Schedule.spaced(10.minutes))
      .fork
      .unit
  }

}
