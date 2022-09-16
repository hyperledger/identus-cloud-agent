package io.iohk.atala.castor.apiserver.worker

import io.iohk.atala.castor.core.model.IrisNotification
import io.iohk.atala.castor.core.service.IrisNotificationService
import zio.*
import zio.stream.{ZSink, ZStream}

class EventConsumer(irisNotificationService: IrisNotificationService) {

  // TODO: replace with actual implementation
  def consumeIrisNotification(source: ZStream[Any, Nothing, IrisNotification]): UIO[Unit] = {
    source.foreach(irisNotificationService.processNotification)
  }

}

object EventConsumer {
  val layer: URLayer[IrisNotificationService, EventConsumer] = ZLayer.fromFunction(new EventConsumer(_))
}
