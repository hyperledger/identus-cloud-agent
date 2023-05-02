package io.iohk.atala.castor.controller

import io.iohk.atala.agent.server.http.model.HttpServiceError
import io.iohk.atala.agent.walletapi.model.error.CreateManagedDIDError
import io.iohk.atala.agent.walletapi.model.error.GetManagedDIDError
import io.iohk.atala.agent.walletapi.model.error.PublishManagedDIDError
import io.iohk.atala.agent.walletapi.model.error.UpdateManagedDIDError
import io.iohk.atala.agent.walletapi.model.ManagedDIDDetail
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.api.http.model.CollectionStats
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.util.PaginationUtils
import io.iohk.atala.castor.controller.http.CreateManagedDidRequest
import io.iohk.atala.castor.controller.http.CreateManagedDIDResponse
import io.iohk.atala.castor.controller.http.DIDOperationResponse
import io.iohk.atala.castor.controller.http.ManagedDID
import io.iohk.atala.castor.controller.http.ManagedDIDPage
import io.iohk.atala.castor.controller.http.UpdateManagedDIDRequest
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.shared.utils.Traverse.*
import zio.*

trait DIDRegistrarController {
  def listManagedDid(paginationInput: PaginationInput)(rc: RequestContext): IO[ErrorResponse, ManagedDIDPage]

  def createManagedDid(request: CreateManagedDidRequest)(
      rc: RequestContext
  ): IO[ErrorResponse, CreateManagedDIDResponse]

  def getManagedDid(did: String)(rc: RequestContext): IO[ErrorResponse, ManagedDID]

  def publishManagedDid(did: String)(rc: RequestContext): IO[ErrorResponse, DIDOperationResponse]

  def updateManagedDid(did: String, updateRequest: UpdateManagedDIDRequest)(
      rc: RequestContext
  ): IO[ErrorResponse, DIDOperationResponse]

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
      ErrorResponse.unprocessableEntity(detail = Some(msg))
    case CreateManagedDIDError.DIDAlreadyExists(did) =>
      ErrorResponse.internalServerError(detail = Some(s"DID already exists: $did"))
    case CreateManagedDIDError.KeyGenerationError(e) =>
      ErrorResponse.internalServerError(detail = Some(e.toString))
    case CreateManagedDIDError.WalletStorageError(e) =>
      ErrorResponse.internalServerError(detail = Some(e.toString))
    case CreateManagedDIDError.InvalidOperation(e) =>
      ErrorResponse.unprocessableEntity(detail = Some(e.toString))
  }

  given Conversion[PublishManagedDIDError, ErrorResponse] = {
    case PublishManagedDIDError.DIDNotFound(did) =>
      ErrorResponse.notFound(detail = Some(s"DID not found: $did"))
    case PublishManagedDIDError.WalletStorageError(e) =>
      ErrorResponse.internalServerError(detail = Some(e.toString))
    case PublishManagedDIDError.OperationError(e) =>
      ErrorResponse.internalServerError(detail = Some(e.toString))
    case PublishManagedDIDError.CryptographyError(e) =>
      ErrorResponse.internalServerError(detail = Some(e.toString))
  }

  given Conversion[UpdateManagedDIDError, ErrorResponse] = {
    case UpdateManagedDIDError.DIDNotFound(did) =>
      ErrorResponse.notFound(detail = Some(s"DID not found: $did"))
    case UpdateManagedDIDError.DIDNotPublished(did) =>
      ErrorResponse.unprocessableEntity(detail = Some(s"DID not published: $did"))
    case UpdateManagedDIDError.DIDAlreadyDeactivated(did) =>
      ErrorResponse.unprocessableEntity(detail = Some(s"DID already deactivated: $did"))
    case UpdateManagedDIDError.InvalidArgument(msg) =>
      ErrorResponse.badRequest(detail = Some(msg))
    case e => ErrorResponse.internalServerError(detail = Some(e.toString))
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
        .mapError(e => ErrorResponse.unprocessableEntity(detail = Some(e)))
      longFormDID <- service
        .createAndStoreDID(didTemplate)
        .mapError[ErrorResponse](e => e)
    } yield CreateManagedDIDResponse(
      longFormDid = longFormDID.toString
    )
  }

  override def getManagedDid(did: String)(rc: RequestContext): IO[ErrorResponse, ManagedDID] = {
    for {
      prismDID <- extractPrismDID(did)
      didDetail <- service
        .getManagedDIDState(prismDID.asCanonical)
        .mapError[ErrorResponse](e => e)
        .someOrFail(ErrorResponse.notFound(detail = Some(s"DID $did was not found in the storage")))
        .map(state => ManagedDIDDetail(prismDID.asCanonical, state))
    } yield didDetail
  }

  override def publishManagedDid(did: String)(rc: RequestContext): IO[ErrorResponse, DIDOperationResponse] = {
    for {
      prismDID <- extractPrismDID(did)
      outcome <- service
        .publishStoredDID(prismDID.asCanonical)
        .mapError[ErrorResponse](e => e)
    } yield outcome
  }

  override def updateManagedDid(did: String, updateRequest: UpdateManagedDIDRequest)(
      rc: RequestContext
  ): IO[ErrorResponse, DIDOperationResponse] = {
    for {
      prismDID <- extractPrismDID(did)
      actions <- ZIO
        .fromEither(updateRequest.actions.traverse(_.toDomain))
        .mapError(e => ErrorResponse.badRequest(detail = Some(e)))
      outcome <- service
        .updateManagedDID(prismDID.asCanonical, actions)
        .mapError[ErrorResponse](e => e)
    } yield outcome
  }

  override def deactivateManagedDid(did: String)(rc: RequestContext): IO[ErrorResponse, DIDOperationResponse] = {
    for {
      prismDID <- extractPrismDID(did)
      outcome <- service
        .deactivateManagedDID(prismDID.asCanonical)
        .mapError[ErrorResponse](e => e)
    } yield outcome
  }

  private def extractPrismDID(did: String): IO[ErrorResponse, PrismDID] =
    ZIO.fromEither(PrismDID.fromString(did)).mapError(e => ErrorResponse.badRequest(detail = Some(e.toString)))

}

object DIDRegistrarControllerImpl {
  val layer: URLayer[ManagedDIDService, DIDRegistrarController] = ZLayer.fromFunction(DIDRegistrarControllerImpl(_))
}
