package org.hyperledger.identus.event.controller

import org.hyperledger.identus.agent.walletapi.service.WalletManagementService
import org.hyperledger.identus.agent.walletapi.service.WalletManagementServiceError
import org.hyperledger.identus.api.http.ErrorResponse
import org.hyperledger.identus.api.http.RequestContext
import org.hyperledger.identus.api.http.model.CollectionStats
import org.hyperledger.identus.api.http.model.PaginationInput
import org.hyperledger.identus.api.util.PaginationUtils
import org.hyperledger.identus.event.controller.http.CreateWebhookNotification
import org.hyperledger.identus.event.controller.http.WebhookNotification
import org.hyperledger.identus.event.controller.http.WebhookNotificationPage
import org.hyperledger.identus.event.notification.EventNotificationConfig
import org.hyperledger.identus.iam.wallet.http.controller.WalletManagementController
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

import java.net.URL
import scala.language.implicitConversions
import java.util.UUID

trait EventController {
  def createWebhookNotification(
      request: CreateWebhookNotification
  )(implicit rc: RequestContext): ZIO[WalletAccessContext, ErrorResponse, WebhookNotification]

  def listWebhookNotifications(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, WebhookNotificationPage]

  def deleteWebhookNotification(id: UUID)(implicit rc: RequestContext): ZIO[WalletAccessContext, ErrorResponse, Unit]
}

object EventController {
  given Conversion[WalletManagementServiceError, ErrorResponse] =
    WalletManagementController.walletServiceErrorConversion
}

class EventControllerImpl(service: WalletManagementService) extends EventController {

  import EventController.given

  override def createWebhookNotification(
      request: CreateWebhookNotification
  )(implicit rc: RequestContext): ZIO[WalletAccessContext, ErrorResponse, WebhookNotification] = {
    for {
      url <- ZIO.attempt(new URL(request.url)).mapError(e => ErrorResponse.badRequest(detail = Some(e.toString())))
      notificationConfig <- EventNotificationConfig.applyWallet(url, request.customHeaders.getOrElse(Map.empty))
      _ <- service
        .createWalletNotification(notificationConfig)
        .mapError[ErrorResponse](e => e)
    } yield notificationConfig
  }

  override def listWebhookNotifications(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, WebhookNotificationPage] = {
    val uri = rc.request.uri
    // No pagination in DB as number of webhook is limited
    // Return paginated result for consistency and to make it future-proof
    val pagination = PaginationInput().toPagination
    for {
      items <- service.listWalletNotifications.mapError[ErrorResponse](e => e)
      totalCount = items.length
      stats = CollectionStats(totalCount = totalCount, filteredCount = totalCount)
    } yield WebhookNotificationPage(
      self = uri.toString(),
      pageOf = PaginationUtils.composePageOfUri(uri).toString,
      next = PaginationUtils.composeNextUri(uri, items, pagination, stats).map(_.toString),
      previous = PaginationUtils.composePreviousUri(uri, items, pagination, stats).map(_.toString),
      contents = items.map(i => i),
    )
  }

  override def deleteWebhookNotification(
      id: UUID
  )(implicit rc: RequestContext): ZIO[WalletAccessContext, ErrorResponse, Unit] = {
    service
      .deleteWalletNotification(id)
      .mapError[ErrorResponse](e => e)
  }

}

object EventControllerImpl {
  val layer: URLayer[WalletManagementService, EventControllerImpl] = ZLayer.fromFunction(EventControllerImpl(_))
}
