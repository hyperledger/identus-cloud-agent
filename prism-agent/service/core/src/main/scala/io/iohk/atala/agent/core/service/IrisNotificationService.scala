package io.iohk.atala.agent.core.service

import io.iohk.atala.agent.core.model.IrisNotification
import zio.*

// TODO: replace with actual implementation
trait IrisNotificationService {
  def processNotification(notification: IrisNotification): UIO[Unit]
}

object MockIrisNotificationService {
  val layer: ULayer[IrisNotificationService] = ZLayer.succeed {
    new IrisNotificationService {
      override def processNotification(notification: IrisNotification): UIO[Unit] = ZIO.unit
    }
  }
}
