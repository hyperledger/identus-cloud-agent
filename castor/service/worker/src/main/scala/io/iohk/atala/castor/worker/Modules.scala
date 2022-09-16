package io.iohk.atala.castor.worker

import io.iohk.atala.castor.core.model.IrisNotification
import io.iohk.atala.castor.core.service.MockIrisNotificationService
import io.iohk.atala.castor.worker.app.EventConsumer
import zio.*
import zio.stream.ZStream

object Modules {

  // TODO: replace with actual implementation
  val irisNotificationSource: UIO[ZStream[Any, Nothing, IrisNotification]] = ZIO.succeed {
    ZStream
      .tick(1.seconds)
      .as(IrisNotification(foo = "bar"))
  }

  val eventConsumerLayer: ULayer[EventConsumer] = {
    val serviceLayer = MockIrisNotificationService.layer // TODO: replace with actual implementation
    serviceLayer >>> EventConsumer.layer
  }

  val app: UIO[Unit] = {
    val consumerApp = for {
      source <- irisNotificationSource
      consumer <- ZIO.service[EventConsumer]
      _ <- consumer.consumeIrisNotification(source)
    } yield ()

    consumerApp.provideLayer(eventConsumerLayer)
  }

}
