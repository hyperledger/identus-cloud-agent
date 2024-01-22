package io.iohk.atala.credentialstatus.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.credentialstatus.controller.http.CredentialStatusList
import io.iohk.atala.pollux.core.service.CredentialStatusListService
import io.iohk.atala.pollux.vc.jwt.{JWT, StatusPurpose}
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

class CredentialStatusControllerImpl(
    credentialStatusListService: CredentialStatusListService
) extends CredentialStatusController {
  def getStatusListJwtCredentialById(id: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, CredentialStatusList] = {
    

    credentialStatusListService
      .findById(id)
      .debug("get status list jwt credential by id")
      .map(CredentialStatusList.fromDomain)
      .mapError(CredentialStatusController.toHttpError)

  }

}

object CredentialStatusControllerImpl {
  val layer: URLayer[CredentialStatusListService, CredentialStatusControllerImpl] =
    ZLayer.fromFunction(CredentialStatusControllerImpl(_))
}
