package io.iohk.atala.castor.controller

import io.iohk.atala.agent.server.http.model.HttpServiceError
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.api.http.model.{PaginationInput, CollectionStats}
import io.iohk.atala.castor.controller.http.ManagedDIDPage
import zio.*
import io.iohk.atala.api.util.PaginationUtils

class DIDRegistrarControllerImpl(service: ManagedDIDService) extends DIDRegistrarController {
  override def listManagedDid(
      requestContext: RequestContext,
      paginationInput: PaginationInput
  ): IO[ErrorResponse, ManagedDIDPage] = {
    val uri = requestContext.request.uri
    val pagination = paginationInput.toPagination
    val result = for {
      pageResult <- service
        .listManagedDIDPage(offset = pagination.offset, limit = pagination.limit)
        .mapError(HttpServiceError.DomainError.apply)
      (items, totalCount) = pageResult
      stats = CollectionStats(totalCount = totalCount, filteredCount = totalCount)
    } yield ManagedDIDPage(
      self = uri.toString(),
      pageOf = PaginationUtils.composePageOfUri(uri).toString,
      next = PaginationUtils.composeNextUri(uri, items, pagination, stats).map(_.toString),
      previous = PaginationUtils.composePreviousUri(uri, items, pagination, stats).map(_.toString),
      contents = items.map(i => i),
    )

    ???
  }
}

object DIDRegistrarControllerImpl {
  val layer: URLayer[ManagedDIDService, DIDRegistrarController] = ZLayer.fromFunction(DIDRegistrarControllerImpl(_))
}
