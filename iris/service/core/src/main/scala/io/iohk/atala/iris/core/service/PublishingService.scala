package io.iohk.atala.iris.core.service

import io.iohk.atala.iris.proto.service as proto
import zio.*

// TODO: replace with actual implementation
trait PublishingService {
  def publishOperation(op: proto.IrisOperation): UIO[Unit] = ???
}

object MockPublishingService {
  val layer: ULayer[PublishingService] = ZLayer.succeed {
    new PublishingService {
      override def publishOperation(op: proto.IrisOperation): UIO[Unit] = ZIO.unit
    }
  }
}
