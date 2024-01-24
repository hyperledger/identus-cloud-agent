package io.iohk.atala.credentialstatus.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.credentialstatus.controller.http.StatusListCredential
import io.iohk.atala.pollux.core.service.CredentialStatusListService
import io.iohk.atala.pollux.vc.jwt.{JWT, StatusPurpose}
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

class CredentialStatusControllerImpl(
    credentialStatusListService: CredentialStatusListService
) extends CredentialStatusController {
  def getStatusListCredentialById(id: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, StatusListCredential] = {

    credentialStatusListService
      .findById(id)
      .map(StatusListCredential.fromCredentialStatusListEntry)
      .mapError(CredentialStatusController.toHttpError)

  }

}

object CredentialStatusControllerImpl {
  val layer: URLayer[CredentialStatusListService, CredentialStatusControllerImpl] =
    ZLayer.fromFunction(CredentialStatusControllerImpl(_))
}
