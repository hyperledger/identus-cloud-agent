package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.W3CCredential
import zio.*

// TODO: replace with actual implementation
trait CredentialRepository[F[_]] {
  def getCredentials: F[Seq[W3CCredential]]
}
