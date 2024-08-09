package org.hyperledger.identus.shared.messaging.kafka

import zio.*
import zio.concurrent.ConcurrentMap
import zio.stream.*
import zio.Clock
import zio.Task
import InMemoryMessagingService.*
import org.hyperledger.identus.shared.messaging.{Consumer, Message, MessagingService, Producer, Serde}

import java.util.concurrent.TimeUnit

case class ConsumerGroupKey(groupId: GroupId, topic: Topic)

class InMemoryMessagingService(
    topicQueues: ConcurrentMap[Topic, (Queue[Message[_, _]], Ref[Offset])],
    queueCapacity: Int,
    processedMessagesMap: ConcurrentMap[
      ConsumerGroupKey,
      ConcurrentMap[Offset, TimeStamp]
    ]
) extends MessagingService {

  override def makeConsumer[K, V](groupId: String)(using kSerde: Serde[K], vSerde: Serde[V]): Task[Consumer[K, V]] = {
    ZIO.succeed(new InMemoryConsumer[K, V](groupId, topicQueues, processedMessagesMap))
  }

  override def makeProducer[K, V]()(using kSerde: Serde[K], vSerde: Serde[V]): Task[Producer[K, V]] =
    ZIO.succeed(new InMemoryProducer[K, V](topicQueues, queueCapacity))
}

class InMemoryConsumer[K, V](
    groupId: GroupId,
    topicQueues: ConcurrentMap[Topic, (Queue[Message[_, _]], Ref[Offset])],
    processedMessagesMap: ConcurrentMap[ConsumerGroupKey, ConcurrentMap[Offset, TimeStamp]]
) extends Consumer[K, V] {
  override def consume[HR](topic: String, topics: String*)(handler: Message[K, V] => URIO[HR, Unit]): RIO[HR, Unit] = {
    val allTopics = topic +: topics
    def getQueueStream(topic: String): ZStream[Any, Nothing, (String, Message[K, V])] =
      ZStream.repeatZIO {
        topicQueues.get(topic).flatMap {
          case Some((queue, _)) =>
            ZIO.debug(s"Connected to queue for topic $topic in group $groupId") *>
              ZIO.succeed(ZStream.fromQueue(queue).collect { case msg: Message[K, V] => (topic, msg) })
          case None =>
            ZIO.sleep(1.second) *> ZIO.succeed(ZStream.empty)
        }
      }.flatten

    val streams = allTopics.map(getQueueStream)
    ZStream
      .mergeAllUnbounded()(streams: _*)
      .tap { case (topic, msg) => ZIO.log(s"Processing message in group $groupId, topic:$topic : $msg") }
      .filterZIO { case (topic, msg) =>
        for {
          currentTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
          key = ConsumerGroupKey(groupId, topic)
          topicProcessedMessages <- processedMessagesMap.get(key).flatMap {
            case Some(map) => ZIO.succeed(map)
            case None =>
              for {
                newMap <- ConcurrentMap.empty[Offset, TimeStamp]
                _ <- processedMessagesMap.put(key, newMap)
              } yield newMap
          }
          isNew <- topicProcessedMessages
            .putIfAbsent(Offset(msg.offset), TimeStamp(currentTime))
            .map(_.isEmpty)
        } yield isNew
      }
      .mapZIO { case (_, msg) => handler(msg) }
      .tap(_ => ZIO.log(s"Message processed in group $groupId, topic:$topic"))
      .runDrain
  }
}

class InMemoryProducer[K, V](
    topicQueues: ConcurrentMap[Topic, (Queue[Message[_, _]], Ref[Offset])],
    queueCapacity: Int
) extends Producer[K, V] {
  override def produce(topic: String, key: K, value: V): Task[Unit] = for {
    queueAndOffsetRef <- topicQueues.get(topic).flatMap {
      case Some(qAndOffSetRef) => ZIO.succeed(qAndOffSetRef)
      case None =>
        for {
          newQueue <- Queue.sliding[Message[_, _]](queueCapacity)
          newOffSetRef <- Ref.make(Offset(0L))
          _ <- topicQueues.put(topic, (newQueue, newOffSetRef))
        } yield (newQueue, newOffSetRef)
    }
    (queue, offsetRef) = queueAndOffsetRef
    currentTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
    messageId <- offsetRef.updateAndGet(x => Offset(x.value + 1)) //  unique atomic id incremented per topic
    _ <- queue.offer(Message(key, value, messageId.value, currentTime))
  } yield ()
}

object InMemoryMessagingService {
  type Topic = String
  type GroupId = String

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
        queueMap <- ConcurrentMap.empty[Topic, (Queue[Message[_, _]], Ref[Offset])]
        processedMessagesMap <- ConcurrentMap.empty[ConsumerGroupKey, ConcurrentMap[Offset, TimeStamp]]
        queueCapacity = 100 // queue capacity
        _ <- cleanupTaskForProcessedMessages(processedMessagesMap)
      } yield new InMemoryMessagingService(queueMap, queueCapacity, processedMessagesMap)
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
      processedMessagesMap: ConcurrentMap[ConsumerGroupKey, ConcurrentMap[Offset, TimeStamp]],
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
      entries <- processedMessagesMap.toList
      _ <- ZIO.foreachDiscard(entries) { case (key, map) =>
        ZIO.log(s"Cleaning up entries for group: ${key.groupId} and topic: ${key.topic}") *>
          cleanupOldEntries(map)
      }
    } yield ())
      .repeat(Schedule.spaced(10.minutes))
      .fork
      .unit
  }
}
