package org.hyperledger.identus.iam.wallet.http.controller

import org.hyperledger.identus.agent.walletapi.model.BaseEntity
import org.hyperledger.identus.agent.walletapi.model.Wallet
import org.hyperledger.identus.agent.walletapi.model.WalletSeed
import org.hyperledger.identus.agent.walletapi.service.WalletManagementService
import org.hyperledger.identus.agent.walletapi.service.WalletManagementServiceError
import org.hyperledger.identus.agent.walletapi.service.WalletManagementServiceError.TooManyPermittedWallet
import org.hyperledger.identus.api.http.ErrorResponse
import org.hyperledger.identus.api.http.RequestContext
import org.hyperledger.identus.api.http.model.CollectionStats
import org.hyperledger.identus.api.http.model.PaginationInput
import org.hyperledger.identus.api.util.PaginationUtils
import org.hyperledger.identus.iam.authentication.oidc.KeycloakEntity
import org.hyperledger.identus.iam.authorization.core.PermissionManagement
import org.hyperledger.identus.iam.wallet.http.model.CreateWalletRequest
import org.hyperledger.identus.iam.wallet.http.model.CreateWalletUmaPermissionRequest
import org.hyperledger.identus.iam.wallet.http.model.WalletDetail
import org.hyperledger.identus.iam.wallet.http.model.WalletDetailPage
import org.hyperledger.identus.shared.models.HexString
import org.hyperledger.identus.shared.models.WalletAdministrationContext
import org.hyperledger.identus.shared.models.WalletAdministrationContext.Admin
import org.hyperledger.identus.shared.models.WalletId
import zio.*

import java.util.UUID
import scala.language.implicitConversions

trait WalletManagementController {
  def listWallet(
      paginationInput: PaginationInput
  )(implicit rc: RequestContext): ZIO[WalletAdministrationContext, ErrorResponse, WalletDetailPage]
  def getWallet(walletId: UUID)(implicit
      rc: RequestContext
  ): ZIO[WalletAdministrationContext, ErrorResponse, WalletDetail]
  def createWallet(
      request: CreateWalletRequest,
      me: BaseEntity
  )(implicit rc: RequestContext): ZIO[WalletAdministrationContext, ErrorResponse, WalletDetail]
  def createWalletUmaPermission(
      walletId: UUID,
      request: CreateWalletUmaPermissionRequest
  )(implicit rc: RequestContext): ZIO[WalletAdministrationContext, ErrorResponse, Unit]
  def deleteWalletUmaPermission(
      walletId: UUID,
      subject: UUID
  )(implicit rc: RequestContext): ZIO[WalletAdministrationContext, ErrorResponse, Unit]
}

object WalletManagementController {
  given walletServiceErrorConversion: Conversion[WalletManagementServiceError, ErrorResponse] = {
    case WalletManagementServiceError.UnexpectedStorageError(cause) =>
      ErrorResponse.internalServerError(detail = Some(cause.getMessage()))
    case WalletManagementServiceError.TooManyWebhookError(limit, actual) =>
      ErrorResponse.conflict(detail = Some(s"Too many webhook created for a wallet. (limit $limit, actual $actual)"))
    case WalletManagementServiceError.DuplicatedWalletId(id) =>
      ErrorResponse.badRequest(s"Wallet id $id is not unique.")
    case WalletManagementServiceError.DuplicatedWalletSeed(id) =>
      ErrorResponse.badRequest(s"Wallet id $id cannot be created. The seed value is not unique.")
    case TooManyPermittedWallet() =>
      ErrorResponse.badRequest(
        s"The operation is not allowed because wallet access already exists for the current user."
      )
  }

  given permissionManagementErrorConversion: Conversion[PermissionManagement.Error, ErrorResponse] = {
    case e: PermissionManagement.Error.PermissionNotFoundById => ErrorResponse.badRequest(detail = Some(e.message))
    case e: PermissionManagement.Error.ServiceError       => ErrorResponse.internalServerError(detail = Some(e.message))
    case e: PermissionManagement.Error.UnexpectedError    => ErrorResponse.internalServerError(detail = Some(e.message))
    case e: PermissionManagement.Error.UserNotFoundById   => ErrorResponse.badRequest(detail = Some(e.message))
    case e: PermissionManagement.Error.WalletNotFoundById => ErrorResponse.badRequest(detail = Some(e.message))
    case e: PermissionManagement.Error.WalletNotFoundByUserId     => ErrorResponse.badRequest(detail = Some(e.message))
    case e: PermissionManagement.Error.WalletResourceNotFoundById => ErrorResponse.badRequest(detail = Some(e.message))
    case e: PermissionManagement.Error.PermissionNotAvailable     => ErrorResponse.badRequest(detail = Some(e.message))
  }
}

class WalletManagementControllerImpl(
    walletService: WalletManagementService,
    permissionService: PermissionManagement.Service[BaseEntity],
) extends WalletManagementController {

  import WalletManagementController.given

  override def listWallet(
      paginationInput: PaginationInput
  )(implicit rc: RequestContext): ZIO[WalletAdministrationContext, ErrorResponse, WalletDetailPage] = {
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

  override def getWallet(
      walletId: UUID
  )(implicit rc: RequestContext): ZIO[WalletAdministrationContext, ErrorResponse, WalletDetail] = {
    for {
      wallet <- walletService
        .getWallet(WalletId.fromUUID(walletId))
        .mapError[ErrorResponse](e => e)
        .someOrFail(ErrorResponse.notFound(detail = Some(s"Wallet id $walletId does not exist.")))
    } yield wallet
  }

  override def createWallet(
      request: CreateWalletRequest,
      me: BaseEntity
  )(implicit rc: RequestContext): ZIO[WalletAdministrationContext, ErrorResponse, WalletDetail] = {
    ZIO.serviceWithZIO[WalletAdministrationContext] {
      case WalletAdministrationContext.Admin() => doCreateWallet(request).map(i => i)
      case WalletAdministrationContext.SelfService(_) =>
        for {
          wallet <- doCreateWallet(request)
          _ <- permissionService
            .grantWalletToUser(wallet.id, me)
            .mapError[ErrorResponse](e => e)
            .provide(ZLayer.succeed(WalletAdministrationContext.Admin())) // First time to use must be admin
        } yield wallet
    }
  }

  override def createWalletUmaPermission(walletId: UUID, request: CreateWalletUmaPermissionRequest)(implicit
      rc: RequestContext
  ): ZIO[WalletAdministrationContext, ErrorResponse, Unit] = {
    val grantee = KeycloakEntity(request.subject)
    permissionService
      .grantWalletToUser(WalletId.fromUUID(walletId), grantee)
      .mapError[ErrorResponse](e => e)
  }

  override def deleteWalletUmaPermission(walletId: UUID, subject: UUID)(implicit
      rc: RequestContext
  ): ZIO[WalletAdministrationContext, ErrorResponse, Unit] = {
    val grantee = KeycloakEntity(subject)
    permissionService
      .revokeWalletFromUser(WalletId.fromUUID(walletId), grantee)
      .mapError[ErrorResponse](e => e)
  }

  private def doCreateWallet(request: CreateWalletRequest): ZIO[WalletAdministrationContext, ErrorResponse, Wallet] = {
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
