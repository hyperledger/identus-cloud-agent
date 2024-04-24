package org.hyperledger.identus.credential.status.controller

import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
import org.hyperledger.identus.credential.status.controller.http.StatusListCredential
import org.hyperledger.identus.pollux.core.service.CredentialStatusListService
import zio.*
import org.hyperledger.identus.pollux.core.model.DidCommID
import org.hyperledger.identus.shared.models.WalletAccessContext

import java.util.UUID

class CredentialStatusControllerImpl(
    credentialStatusListService: CredentialStatusListService
) extends CredentialStatusController {
  def getStatusListCredentialById(id: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, StatusListCredential] = {

    credentialStatusListService
      .findById(id)
      .flatMap(StatusListCredential.fromCredentialStatusListEntry)
      .mapError(CredentialStatusController.toHttpError)

  }

  def revokeCredentialById(id: DidCommID)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, Unit] = {
    credentialStatusListService
      .revokeByIssueCredentialRecordId(id)
      .mapError(CredentialStatusController.toHttpError)
  }

}

object CredentialStatusControllerImpl {
  val layer: URLayer[CredentialStatusListService, CredentialStatusControllerImpl] =
    ZLayer.fromFunction(CredentialStatusControllerImpl(_))
}
