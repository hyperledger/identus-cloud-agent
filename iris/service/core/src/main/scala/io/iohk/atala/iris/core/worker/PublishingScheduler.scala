package io.iohk.atala.iris.core.worker

import io.iohk.atala.iris.proto.dlt as proto
import zio.{UIO, ULayer, ZIO, ZLayer}

trait PublishingScheduler {
  def scheduleOperations(op: proto.IrisOperation): UIO[Unit]
}

object MockPublishingScheduler {
  val layer: ULayer[PublishingScheduler] = ZLayer.succeed {
    new PublishingScheduler {
      def scheduleOperations(op: proto.IrisOperation): UIO[Unit] = ZIO.unit
    }
  }
}
