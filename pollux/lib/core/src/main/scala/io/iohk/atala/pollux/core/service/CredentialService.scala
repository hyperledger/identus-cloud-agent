package io.iohk.atala.pollux.core.service

import zio.*

// TODO: replace with actual implementation
trait CredentialService {
  def getCredential(did: String): UIO[Unit]
}

object MockCredentialService {
  val layer: ULayer[CredentialService] = ZLayer.succeed {
    new CredentialService {
      override def getCredential(did: String): UIO[Unit] = ZIO.unit
    }
  }
}
