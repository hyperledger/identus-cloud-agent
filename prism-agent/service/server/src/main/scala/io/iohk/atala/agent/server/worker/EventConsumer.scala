package io.iohk.atala.agent.server.worker

import io.iohk.atala.agent.core.model.IrisNotification
import io.iohk.atala.agent.core.service.IrisNotificationService
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
