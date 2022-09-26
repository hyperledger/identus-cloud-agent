package io.iohk.atala.iris.core.service

import io.iohk.atala.iris.core.model.SignedIrisOperation
import zio.*

// TODO: replace with actual implementation
trait PublishingService {
  def publishOperations(op: SignedIrisOperation): UIO[Unit]
}

object MockPublishingService {
  val layer: ULayer[PublishingService] = ZLayer.succeed {
    new PublishingService {
      override def publishOperations(op: SignedIrisOperation): UIO[Unit] = ZIO.unit
    }
  }
}
