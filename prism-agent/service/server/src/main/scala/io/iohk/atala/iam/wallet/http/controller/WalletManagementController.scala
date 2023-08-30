package io.iohk.atala.iam.wallet.http.controller

import io.iohk.atala.agent.walletapi.model.Wallet
import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.agent.walletapi.service.WalletManagementServiceError
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.CollectionStats
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.util.PaginationUtils
import io.iohk.atala.iam.wallet.http.model.CreateWalletRequest
import io.iohk.atala.iam.wallet.http.model.WalletDetail
import io.iohk.atala.iam.wallet.http.model.WalletDetailPage
import io.iohk.atala.shared.models.HexString
import zio.*

import java.util.UUID
import scala.language.implicitConversions
import io.iohk.atala.shared.models.WalletId

trait WalletManagementController {
  def listWallet(paginationInput: PaginationInput)(implicit rc: RequestContext): IO[ErrorResponse, WalletDetailPage]
  def getWallet(walletId: UUID)(implicit rc: RequestContext): IO[ErrorResponse, WalletDetail]
  def createWallet(request: CreateWalletRequest)(implicit rc: RequestContext): IO[ErrorResponse, WalletDetail]
}

object WalletManagementController {
  given Conversion[WalletManagementServiceError, ErrorResponse] = {
    case WalletManagementServiceError.SeedGenerationError(cause) =>
      ErrorResponse.internalServerError(detail = Some(cause.toString()))
    case WalletManagementServiceError.WalletStorageError(cause) =>
      ErrorResponse.internalServerError(detail = Some(cause.toString()))
  }
}

class WalletManagementControllerImpl(
    service: WalletManagementService
) extends WalletManagementController {

  import WalletManagementController.given

  override def listWallet(
      paginationInput: PaginationInput
  )(implicit rc: RequestContext): IO[ErrorResponse, WalletDetailPage] = {
    val uri = rc.request.uri
    val pagination = paginationInput.toPagination
    for {
      pageResult <- service
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
      wallet <- service
        .getWallet(WalletId.fromUUID(walletId))
        .mapError[ErrorResponse](e => e)
        .someOrFail(ErrorResponse.notFound(detail = Some(s"Wallet id $walletId does not exist.")))
    } yield wallet
  }

  override def createWallet(
      request: CreateWalletRequest
  )(implicit rc: RequestContext): IO[ErrorResponse, WalletDetail] = {
    for {
      providedSeed <- request.seed.fold(ZIO.none)(s => extractWalletSeed(s).asSome)
      wallet <- service.createWallet(Wallet(request.name), providedSeed).mapError[ErrorResponse](e => e)
    } yield wallet
  }

  private def extractWalletSeed(seedHex: String): IO[ErrorResponse, WalletSeed] = {
    ZIO
      .fromTry(HexString.fromString(seedHex))
      .mapError(_.getMessage())
      .map(_.toByteArray)
      .map(WalletSeed.fromByteArray)
      .absolve
      .mapError(e => ErrorResponse.badRequest(detail = Some(e)))
  }

}

object WalletManagementControllerImpl {
  val layer: URLayer[WalletManagementService, WalletManagementController] =
    ZLayer.fromFunction(WalletManagementControllerImpl(_))
}
