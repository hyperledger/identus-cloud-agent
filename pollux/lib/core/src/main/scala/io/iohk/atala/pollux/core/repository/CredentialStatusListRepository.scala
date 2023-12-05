package io.iohk.atala.pollux.core.repository

import io.iohk.atala.mercury.protocol.issuecredential.{IssueCredential, RequestCredential}
import io.iohk.atala.pollux.anoncreds.CredentialRequestMetadata
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.ProtocolState
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import zio.*

trait CredentialStatusListRepository {
  def getLatestOfTheWallet(walletId: WalletId): RIO[WalletAccessContext, CredentialStatusList]

}
