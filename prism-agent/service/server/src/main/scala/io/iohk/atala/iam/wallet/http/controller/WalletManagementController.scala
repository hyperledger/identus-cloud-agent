package io.iohk.atala.iam.wallet.http.controller

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.agent.walletapi.service.WalletManagementServiceError
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.CollectionStats
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.util.PaginationUtils
import io.iohk.atala.iam.authorization.core.PermissionManagement
import io.iohk.atala.iam.wallet.http.model.CreateWalletRequest
import io.iohk.atala.iam.wallet.http.model.WalletDetail
import io.iohk.atala.iam.wallet.http.model.WalletDetailPage
import io.iohk.atala.shared.models.HexString
import io.iohk.atala.shared.models.WalletId
import zio.*

import java.util.UUID
import scala.language.implicitConversions
import io.iohk.atala.agent.walletapi.model.EntityRole

trait WalletManagementController {
  def listWallet(paginationInput: PaginationInput)(implicit rc: RequestContext): IO[ErrorResponse, WalletDetailPage]
  def getWallet(walletId: UUID)(implicit rc: RequestContext): IO[ErrorResponse, WalletDetail]
  def createWallet(
      request: CreateWalletRequest
  )(implicit rc: RequestContext, entity: BaseEntity): IO[ErrorResponse, WalletDetail]
}

object WalletManagementController {
  given walletServiceErrorConversion: Conversion[WalletManagementServiceError, ErrorResponse] = {
    case WalletManagementServiceError.SeedGenerationError(cause) =>
      ErrorResponse.internalServerError(detail = Some(cause.getMessage()))
    case WalletManagementServiceError.UnexpectedStorageError(cause) =>
      ErrorResponse.internalServerError(detail = Some(cause.getMessage()))
    case WalletManagementServiceError.TooManyWebhookError(limit, actual) =>
      ErrorResponse.conflict(detail = Some(s"Too many webhook created for a wallet. (limit $limit, actual $actual)"))
    case WalletManagementServiceError.DuplicatedWalletId(id) =>
      ErrorResponse.badRequest(s"Wallet id $id is not unique.")
    case WalletManagementServiceError.DuplicatedWalletSeed(id) =>
      ErrorResponse.badRequest(s"Wallet id $id cannot be created. The seed value is not unique.")
  }

  given permissionManagementErrorConversion: Conversion[PermissionManagement.Error, ErrorResponse] = {
    case e: PermissionManagement.Error.PermissionNotFoundById => ErrorResponse.badRequest(detail = Some(e.message))
    case e: PermissionManagement.Error.ServiceError       => ErrorResponse.internalServerError(detail = Some(e.message))
    case e: PermissionManagement.Error.UnexpectedError    => ErrorResponse.internalServerError(detail = Some(e.message))
    case e: PermissionManagement.Error.UserNotFoundById   => ErrorResponse.badRequest(detail = Some(e.message))
    case e: PermissionManagement.Error.WalletNotFoundById => ErrorResponse.badRequest(detail = Some(e.message))
    case e: PermissionManagement.Error.WalletNotFoundByUserId     => ErrorResponse.badRequest(detail = Some(e.message))
    case e: PermissionManagement.Error.WalletResourceNotFoundById => ErrorResponse.badRequest(detail = Some(e.message))
  }
}

class WalletManagementControllerImpl(
    walletService: WalletManagementService,
    permissionService: PermissionManagement.Service[BaseEntity],
) extends WalletManagementController {

  import WalletManagementController.given

  override def listWallet(
      paginationInput: PaginationInput
  )(implicit rc: RequestContext): IO[ErrorResponse, WalletDetailPage] = {
    val uri = rc.request.uri
    val pagination = paginationInput.toPagination
    for {
      pageResult <- walletService
        .listWallets(offset = paginationInput.offset, limit = paginationInput.limit)
        .mapError[ErrorResponse](e => e)
      (items, totalCount) = pageResult
      stats = CollectionStats(totalCount = totalCount, filteredCount = totalCount)
    } yield WalletDetailPage(
      self = uri.toString(),
      pageOf = PaginationUtils.composePageOfUri(uri).toString,
      next = PaginationUtils.composeNextUri(uri, items, pagination, stats).map(_.toString),
      previous = PaginationUtils.composePreviousUri(uri, items, pagination, stats).map(_.toString),
      contents = items.map(i => i),
    )
  }

  override def getWallet(walletId: UUID)(implicit rc: RequestContext): IO[ErrorResponse, WalletDetail] = {
    for {
      wallet <- walletService
        .getWallet(WalletId.fromUUID(walletId))
        .mapError[ErrorResponse](e => e)
        .someOrFail(ErrorResponse.notFound(detail = Some(s"Wallet id $walletId does not exist.")))
    } yield wallet
  }

  override def createWallet(
      request: CreateWalletRequest
  )(implicit rc: RequestContext, entity: BaseEntity): IO[ErrorResponse, WalletDetail] = {
    entity.role match {
      case EntityRole.Admin => doCreateWallet(request).map(i => i)
      case EntityRole.Tenant =>
        for {
          _ <- permissionService
            .listWalletPermissions(entity)
            .mapError[ErrorResponse](e => e)
            .filterOrFail(_.isEmpty)(ErrorResponse.badRequest(detail = Some("Wallet permission already exists.")))
          wallet <- doCreateWallet(request)
          _ <- permissionService.grantWalletToUser(wallet.id, entity).mapError[ErrorResponse](e => e)
        } yield wallet
    }
  }

  private def doCreateWallet(request: CreateWalletRequest): IO[ErrorResponse, Wallet] = {
    for {
      providedSeed <- request.seed.fold(ZIO.none)(s => extractWalletSeed(s).asSome)
      walletId = request.id.map(WalletId.fromUUID).getOrElse(WalletId.random)
      wallet <- walletService
        .createWallet(Wallet(request.name, walletId), providedSeed)
        .mapError[ErrorResponse](identity)
    } yield wallet
  }

  private def extractWalletSeed(seedHex: String): IO[ErrorResponse, WalletSeed] = {
    ZIO
      .fromTry(HexString.fromString(seedHex))
      .mapError(_.getMessage())
      .map(_.toByteArray)
      .map(WalletSeed.fromByteArray)
      .absolve
      .mapError(e =>
        ErrorResponse.badRequest(detail =
          Some(s"The provided wallet seed is not valid hex string representing a BIP-32 seed. ($e)")
        )
      )
  }

}

object WalletManagementControllerImpl {
  val layer: URLayer[WalletManagementService & PermissionManagement.Service[BaseEntity], WalletManagementController] =
    ZLayer.fromFunction(WalletManagementControllerImpl(_, _))
}
