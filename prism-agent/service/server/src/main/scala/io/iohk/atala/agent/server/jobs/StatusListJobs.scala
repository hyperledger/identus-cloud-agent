package io.iohk.atala.agent.server.jobs

import io.iohk.atala.pollux.core.model.error.CredentialStatusListServiceError
import io.iohk.atala.pollux.core.service.CredentialStatusListService
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

object StatusListJobs extends BackgroundJobsHelper {

  val syncRevocationStatuses =
    for {
      credentialStatusListService <- ZIO.service[CredentialStatusListService]
      entries <- credentialStatusListService.getCredentialsAndItsStatuses.debug
      _ <- ZIO.logInfo("Syncing revocation statuses")
    } yield ()
}
