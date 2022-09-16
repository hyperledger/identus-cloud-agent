package io.iohk.atala.castor.core.service

import zio.*

// TODO: replace with actual implementation
trait DIDOperationService {
  def getDIDOperations(did: String): UIO[Unit]
}

object MockDIDOperationService {
  val layer: ULayer[DIDOperationService] = ZLayer.succeed {
    new DIDOperationService {
      override def getDIDOperations(did: String): UIO[Unit] = ZIO.unit
    }
  }
}
