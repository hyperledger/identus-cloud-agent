package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.anoncreds.LinkSecretWithId
import io.iohk.atala.pollux.core.model.error.LinkSecretError
import io.iohk.atala.shared.models.WalletAccessContext
import zio.ZIO

trait LinkSecretService {
  type Result[T] = ZIO[WalletAccessContext, LinkSecretError, T]

  def fetchOrCreate(): Result[LinkSecretWithId]
}
