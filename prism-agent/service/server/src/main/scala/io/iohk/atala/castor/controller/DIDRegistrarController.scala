package io.iohk.atala.castor.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.castor.controller.http.ManagedDIDPage
import zio.*

trait DIDRegistrarController {
  def listManagedDid(
      requestContext: RequestContext,
      paginationInput: PaginationInput
  ): IO[ErrorResponse, ManagedDIDPage]
}
