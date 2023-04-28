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
import io.iohk.atala.agent.walletapi.model.error.CreateManagedDIDError
import io.iohk.atala.castor.controller.http.ManagedDID
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.agent.walletapi.model.ManagedDIDDetail
import io.iohk.atala.castor.controller.http.DIDOperationResponse

trait DIDRegistrarController {
  def listManagedDid(paginationInput: PaginationInput)(rc: RequestContext): IO[ErrorResponse, ManagedDIDPage]

  def createManagedDid(request: CreateManagedDidRequest)(
      rc: RequestContext
  ): IO[ErrorResponse, CreateManagedDIDResponse]

  def getManagedDid(did: String)(rc: RequestContext): IO[ErrorResponse, ManagedDID]

  def publishManagedDid(did: String)(rc: RequestContext): IO[ErrorResponse, DIDOperationResponse]

  def updateManagedDid(did: String)(rc: RequestContext): IO[ErrorResponse, DIDOperationResponse]

  def deactivateManagedDid(did: String)(rc: RequestContext): IO[ErrorResponse, DIDOperationResponse]
}

object DIDRegistrarController {
  given Conversion[GetManagedDIDError, ErrorResponse] = {
    case GetManagedDIDError.OperationError(e) =>
      ErrorResponse.internalServerError(detail = Some(e.toString))
    case GetManagedDIDError.WalletStorageError(e) =>
      ErrorResponse.internalServerError(detail = Some(e.toString))
  }

  given Conversion[CreateManagedDIDError, ErrorResponse] = {
    case CreateManagedDIDError.InvalidArgument(msg) =>
      ErrorResponse.badRequest(detail = Some(msg))
    case CreateManagedDIDError.DIDAlreadyExists(did) =>
      ErrorResponse.internalServerError(detail = Some(s"DID already exists: $did"))
    case CreateManagedDIDError.KeyGenerationError(e) =>
      ErrorResponse.internalServerError(detail = Some(e.toString))
    case CreateManagedDIDError.WalletStorageError(e) =>
      ErrorResponse.internalServerError(detail = Some(e.toString))
    case CreateManagedDIDError.InvalidOperation(e) =>
      ErrorResponse.internalServerError(detail = Some(e.toString))
  }
}

class DIDRegistrarControllerImpl(service: ManagedDIDService) extends DIDRegistrarController {

  import DIDRegistrarController.given

  override def listManagedDid(
      paginationInput: PaginationInput
  )(rc: RequestContext): IO[ErrorResponse, ManagedDIDPage] = {
    val uri = rc.request.uri
    val pagination = paginationInput.toPagination
    for {
      pageResult <- service
        .listManagedDIDPage(offset = pagination.offset, limit = pagination.limit)
        .mapError[ErrorResponse](e => e)
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

  override def createManagedDid(createManagedDidRequest: CreateManagedDidRequest)(
      rc: RequestContext
  ): IO[ErrorResponse, CreateManagedDIDResponse] = {
    for {
      didTemplate <- ZIO
        .fromEither(createManagedDidRequest.documentTemplate.toDomain)
        .mapError(e => ErrorResponse.badRequest(detail = Some(e)))
      longFormDID <- service
        .createAndStoreDID(didTemplate)
        .mapError[ErrorResponse](e => e)
    } yield CreateManagedDIDResponse(
      longFormDid = longFormDID.toString
    )
  }

  override def getManagedDid(did: String)(rc: RequestContext): IO[ErrorResponse, ManagedDID] = {
    for {
      prismDID <- ZIO
        .fromEither(PrismDID.fromString(did))
        .mapError(e => ErrorResponse.badRequest(detail = Some(e)))
      didDetail <- service
        .getManagedDIDState(prismDID.asCanonical)
        .mapError[ErrorResponse](e => e)
        .someOrFail(ErrorResponse.notFound(detail = Some(s"DID $did was not found in the storage")))
        .map(state => ManagedDIDDetail(prismDID.asCanonical, state))
    } yield didDetail
  }

  override def publishManagedDid(did: String)(rc: RequestContext): IO[ErrorResponse, DIDOperationResponse] = ???

  override def updateManagedDid(did: String)(rc: RequestContext): IO[ErrorResponse, DIDOperationResponse] = ???

  override def deactivateManagedDid(did: String)(rc: RequestContext): IO[ErrorResponse, DIDOperationResponse] = ???
}

object DIDRegistrarControllerImpl {
  val layer: URLayer[ManagedDIDService, DIDRegistrarController] = ZLayer.fromFunction(DIDRegistrarControllerImpl(_))
}
