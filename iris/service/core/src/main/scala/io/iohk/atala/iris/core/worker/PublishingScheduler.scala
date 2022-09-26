package io.iohk.atala.iris.core.worker

import io.iohk.atala.iris.core.model.SignedIrisOperation
import io.iohk.atala.iris.core.service.PublishingService
import zio.{UIO, ULayer, ZIO, ZLayer}

trait PublishingScheduler {
  def scheduleOperations(op: SignedIrisOperation): UIO[Unit]
}

object MockPublishingScheduler {
  val layer: ULayer[PublishingScheduler] = ZLayer.succeed {
    new PublishingScheduler {
      def scheduleOperations(op: SignedIrisOperation): UIO[Unit] = ZIO.unit
    }
  }
}
