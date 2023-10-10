package io.iohk.atala.event.controller

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic
import io.iohk.atala.shared.models.WalletAccessContext
import sttp.tapir.ztapir.*
import zio.*

class EventServerEndpoints(
    eventController: EventController,
    authenticator: Authenticator[BaseEntity]
) {

  val createWebhookNotificationServerEndpoint: ZServerEndpoint[Any, Any] =
    EventEndpoints.createWebhookNotification
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (rc, createWebhook) =>
          eventController
            .createWebhookNotification(createWebhook)(rc)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val listWebhookNotificationServerEndpoint: ZServerEndpoint[Any, Any] =
    EventEndpoints.listWebhookNotification
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity => rc =>
        eventController
          .listWebhookNotifications(rc)
          .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
      }

  val deleteWebhookNotificationServerEndpoint: ZServerEndpoint[Any, Any] =
    EventEndpoints.deleteWebhookNotification
      .zServerSecurityLogic(ApiKeyEndpointSecurityLogic.securityLogic(_)(authenticator))
      .serverLogic { entity =>
        { case (rc, id) =>
          eventController
            .deleteWebhookNotification(id)(rc)
            .provideSomeLayer(ZLayer.succeed(entity.walletAccessContext))
        }
      }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    createWebhookNotificationServerEndpoint,
    listWebhookNotificationServerEndpoint,
    deleteWebhookNotificationServerEndpoint
  )

}

object EventServerEndpoints {
  def all: URIO[EventController & DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] = {
    for {
      authenticator <- ZIO.service[DefaultAuthenticator]
      eventController <- ZIO.service[EventController]
      eventEndpoints = new EventServerEndpoints(eventController, authenticator)
    } yield eventEndpoints.all
  }
}
