package io.iohk.atala.castor.controller

import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.castor.controller.http.ManagedDIDPage
import zio.*
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.agent.server.http.model.HttpServiceError
import io.iohk.atala.api.http.model.CollectionStats
import io.iohk.atala.api.util.PaginationUtils
import io.iohk.atala.agent.walletapi.model.error.GetManagedDIDError
import io.iohk.atala.castor.controller.http.CreateManagedDidRequest
import io.iohk.atala.castor.controller.http.CreateManagedDIDResponse

trait DIDRegistrarController {
  def createManagedDid(request: CreateManagedDidRequest)(
      rc: RequestContext
  ): IO[ErrorResponse, CreateManagedDIDResponse]
  def listManagedDid(paginationInput: PaginationInput)(rc: RequestContext): IO[ErrorResponse, ManagedDIDPage]
}

object DIDRegistrarController {
  given Conversion[GetManagedDIDError, ErrorResponse] = {
    case GetManagedDIDError.OperationError(e) =>
      ErrorResponse.internalServerError(title = "DIDOperationError", detail = Some(e.toString))
    case GetManagedDIDError.WalletStorageError(e) =>
      ErrorResponse.internalServerError(title = "StorageError", detail = Some(e.toString))
  }
}

class DIDRegistrarControllerImpl(service: ManagedDIDService) extends DIDRegistrarController {

  import DIDRegistrarController.given

  override def createManagedDid(createManagedDidRequest: CreateManagedDidRequest)(
      rc: RequestContext
  ): IO[ErrorResponse, CreateManagedDIDResponse] = {
    val result = for {
      didTemplate <- ZIO
        .fromEither(createManagedDidRequest.documentTemplate.toDomain)
        .mapError(HttpServiceError.InvalidPayload.apply)
      longFormDID <- service
        .createAndStoreDID(didTemplate)
        .mapError(HttpServiceError.DomainError.apply)
    } yield CreateManagedDIDResponse(
      longFormDid = longFormDID.toString
    )

    ???
  }

  override def listManagedDid(
      paginationInput: PaginationInput
  )(rc: RequestContext): IO[ErrorResponse, ManagedDIDPage] = {
    val uri = rc.request.uri
    val pagination = paginationInput.toPagination
    for {
      _ <- ZIO.debug(uri.toString())
      pageResult <- service
        .listManagedDIDPage(offset = pagination.offset, limit = pagination.limit)
        .mapError[ErrorResponse](i => i)
      (items, totalCount) = pageResult
      stats = CollectionStats(totalCount = totalCount, filteredCount = totalCount)
    } yield ManagedDIDPage(
      self = uri.toString(),
      pageOf = PaginationUtils.composePageOfUri(uri).toString,
      next = PaginationUtils.composeNextUri(uri, items, pagination, stats).map(_.toString),
      previous = PaginationUtils.composePreviousUri(uri, items, pagination, stats).map(_.toString),
      contents = items.map(i => i),
    )
  }
}

object DIDRegistrarControllerImpl {
  val layer: URLayer[ManagedDIDService, DIDRegistrarController] = ZLayer.fromFunction(DIDRegistrarControllerImpl(_))
}
