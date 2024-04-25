package org.hyperledger.identus.pollux.core.service

import org.hyperledger.identus.pollux.anoncreds.AnoncredLinkSecretWithId
import org.hyperledger.identus.pollux.core.model.error.LinkSecretError
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.ZIO

trait LinkSecretService {
  type Result[T] = ZIO[WalletAccessContext, LinkSecretError, T]

  def fetchOrCreate(): Result[AnoncredLinkSecretWithId]
}
