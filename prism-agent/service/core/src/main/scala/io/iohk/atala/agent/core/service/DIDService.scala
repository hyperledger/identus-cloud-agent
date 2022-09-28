package io.iohk.atala.agent.core.service

import zio.*

// TODO: replace with actual implementation
trait DIDService {
  def resolveDID(did: String): UIO[Unit]
}

object MockDIDService {
  val layer: ULayer[DIDService] = ZLayer.succeed {
    new DIDService {
      override def resolveDID(did: String): UIO[Unit] = ZIO.unit
    }
  }
}
