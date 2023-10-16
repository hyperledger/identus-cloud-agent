package io.iohk.atala.event.controller

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.Authorizer
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.shared.models.WalletAccessContext
import sttp.tapir.ztapir.*
import zio.*
import io.iohk.atala.iam.authentication.SecurityLogic

class EventServerEndpoints(
    eventController: EventController,
    authenticator: Authenticator[BaseEntity] & Authorizer[BaseEntity]
) {

  val createWebhookNotificationServerEndpoint: ZServerEndpoint[Any, Any] =
    EventEndpoints.createWebhookNotification
      .zServerSecurityLogic(SecurityLogic.authorizeWith(_)(authenticator))
      .serverLogic { wac =>
        { case (rc, createWebhook) =>
          eventController
            .createWebhookNotification(createWebhook)(rc)
            .provideSomeLayer(ZLayer.succeed(wac))
        }
      }

  val listWebhookNotificationServerEndpoint: ZServerEndpoint[Any, Any] =
    EventEndpoints.listWebhookNotification
      .zServerSecurityLogic(SecurityLogic.authorizeWith(_)(authenticator))
      .serverLogic { wac => rc =>
        eventController
          .listWebhookNotifications(rc)
          .provideSomeLayer(ZLayer.succeed(wac))
      }

  val deleteWebhookNotificationServerEndpoint: ZServerEndpoint[Any, Any] =
    EventEndpoints.deleteWebhookNotification
      .zServerSecurityLogic(SecurityLogic.authorizeWith(_)(authenticator))
      .serverLogic { wac =>
        { case (rc, id) =>
          eventController
            .deleteWebhookNotification(id)(rc)
            .provideSomeLayer(ZLayer.succeed(wac))
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
