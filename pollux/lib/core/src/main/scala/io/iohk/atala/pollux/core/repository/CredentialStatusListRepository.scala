package io.iohk.atala.pollux.core.repository

import io.iohk.atala.castor.core.model.did.CanonicalPrismDID
import io.iohk.atala.mercury.protocol.issuecredential.{IssueCredential, RequestCredential}
import io.iohk.atala.pollux.anoncreds.CredentialRequestMetadata
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.ProtocolState
import io.iohk.atala.pollux.vc.jwt.Issuer
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import zio.*

import java.util.UUID

trait CredentialStatusListRepository {
  def getLatestOfTheWallet(walletId: WalletId): RIO[WalletAccessContext, Option[CredentialStatusList]]

  def createNewForTheWallet(walletId: WalletId,  jwtIssuer: Issuer): RIO[WalletAccessContext, CredentialStatusList]

  def allocateSpaceForCredential(
      issueCredentialRecordId: DidCommID,
      credentialStatusListId: UUID,
      statusListIndex: Int
  ): RIO[WalletAccessContext, Unit]

}
