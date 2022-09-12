package io.iohk.atala.castor.core.service

import zio.*

// TODO: replace with actual implementation
trait DIDService {
  def resolveDID(did: String): UIO[Unit]
}

object MockDIDService {
  val layer: ULayer[DIDService] = ZLayer.succeed {
    new DIDService:
      override def resolveDID(did: String): UIO[Unit] = ZIO.unit
  }
}
