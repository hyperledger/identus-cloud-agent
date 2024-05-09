package org.hyperledger.identus.pollux.core.model.oid4vci

import java.net.URL
import java.time.Instant
import java.util.UUID
import java.time.temporal.ChronoUnit

case class CredentialIssuer(id: UUID, authorizationServer: URL, createdAt: Instant, updatedAt: Instant) {
  def withTruncatedTimestamp(unit: ChronoUnit = ChronoUnit.MICROS): CredentialIssuer = copy(
    createdAt = createdAt.truncatedTo(unit),
    updatedAt = updatedAt.truncatedTo(unit),
  )
}

object CredentialIssuer {
  def apply(authorizationServer: URL): CredentialIssuer = {
    val now = Instant.now
    CredentialIssuer(UUID.randomUUID(), authorizationServer, now, now)
  }
}
