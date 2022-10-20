package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.model.CredentialError.RepositoryError
import io.iohk.atala.pollux.core.model.{CredentialError, JWTCredential}
import io.iohk.atala.pollux.core.repository.CredentialRepository
import zio.*

import java.util.UUID

trait CredentialService {
  def createCredentials(batchId: String, credentials: Seq[JWTCredential]): IO[CredentialError, Unit]
  def getCredentials(batchId: String): IO[CredentialError, Seq[JWTCredential]]
}

object MockCredentialService {
  val layer: ULayer[CredentialService] = ZLayer.succeed {
    new CredentialService {
      override def createCredentials(batchId: String, credentials: Seq[JWTCredential]): IO[CredentialError, Unit] =
        ZIO.succeed(())

      override def getCredentials(did: String): IO[CredentialError, Seq[JWTCredential]] = ZIO.succeed(Nil)
    }
  }
}

object CredentialServiceImpl {
  val layer = ZLayer.fromFunction(CredentialServiceImpl(_))
}

private class CredentialServiceImpl(credentialRepository: CredentialRepository[Task]) extends CredentialService {
  override def getCredentials(batchId: String): IO[CredentialError, Seq[JWTCredential]] = {
    credentialRepository.getCredentials(batchId).mapError(RepositoryError.apply)
  }

  override def createCredentials(batchId: String, credentials: Seq[JWTCredential]): IO[CredentialError, Unit] = {
    credentialRepository.createCredentials(batchId, credentials).mapError(RepositoryError.apply)
  }
}
