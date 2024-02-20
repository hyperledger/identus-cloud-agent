package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.model.CredentialStatusList
import io.iohk.atala.pollux.core.model.error.CredentialStatusListServiceError
import zio.*
import io.iohk.atala.pollux.core.model.DidCommID
import io.iohk.atala.shared.models.WalletAccessContext

import java.util.UUID

trait CredentialStatusListService {
  def findById(id: UUID): IO[CredentialStatusListServiceError, CredentialStatusList]
  
  def revokeByIssueCredentialRecordId(id: DidCommID): ZIO[WalletAccessContext, CredentialStatusListServiceError, Unit]
}
