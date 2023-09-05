package io.iohk.atala.event.controller

import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic
import io.iohk.atala.shared.models.WalletAccessContext
import sttp.tapir.ztapir.*
import zio.*

class EventServerEndpoints(
    eventController: EventController,
    authenticator: Authenticator
) {

  private val createWebhookNotificationServerEndpoint: ZServerEndpoint[Any, Any] =
    EventEndpoints.createWebhookNotification
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (rc, createWebhook) =>
          eventController
            .createWebhookNotification(createWebhook)(rc)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    createWebhookNotificationServerEndpoint
  )

}

object EventServerEndpoints {
  def all: URIO[EventController & Authenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      authenticator <- ZIO.service[Authenticator]
      eventController <- ZIO.service[EventController]
      eventEndpoints = new EventServerEndpoints(eventController, authenticator)
    } yield eventEndpoints.all
  }
}
