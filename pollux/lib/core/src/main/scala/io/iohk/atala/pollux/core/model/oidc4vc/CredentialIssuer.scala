package io.iohk.atala.pollux.core.model.oidc4vc

import java.net.URL
import java.time.Instant
import java.util.UUID

case class CredentialIssuer(id: UUID, authorizationServer: URL, createdAt: Instant, updatedAt: Instant)

object CredentialIssuer {
  def apply(authorizationServer: URL): CredentialIssuer = {
    val now = Instant.now
    CredentialIssuer(UUID.randomUUID(), authorizationServer, now, now)
  }
}
