package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.JWTCredential
import zio.*
trait CredentialRepository[F[_]] {
  def createCredentials(batchId: String, credentials: Seq[JWTCredential]): F[Unit]
  def getCredentials(batchId: String): F[Seq[JWTCredential]]
}
