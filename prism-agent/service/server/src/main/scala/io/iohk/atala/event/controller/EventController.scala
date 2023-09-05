package io.iohk.atala.event.controller

import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.agent.walletapi.service.WalletManagementServiceError
import io.iohk.atala.api.http.ErrorResponse
import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.event.controller.http.CreateWebhookNotification
import io.iohk.atala.event.controller.http.WebhookNotification
import io.iohk.atala.event.notification.EventNotificationConfig
import io.iohk.atala.iam.wallet.http.controller.WalletManagementController
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.net.URL
import scala.language.implicitConversions

trait EventController {
  def createWebhookNotification(
      request: CreateWebhookNotification
  )(implicit rc: RequestContext): ZIO[WalletAccessContext, ErrorResponse, WebhookNotification]
}

object EventController {
  // should we seggregate all the way to all layers?
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
      notificationConfig <- EventNotificationConfig(url, request.customHeaders.getOrElse(Map.empty))
      _ <- service
        .createWalletNotification(notificationConfig)
        .mapError[ErrorResponse](e => e)
    } yield notificationConfig
  }

}

object EventControllerImpl {
  val layer: URLayer[WalletManagementService, EventControllerImpl] = ZLayer.fromFunction(EventControllerImpl(_))
}
