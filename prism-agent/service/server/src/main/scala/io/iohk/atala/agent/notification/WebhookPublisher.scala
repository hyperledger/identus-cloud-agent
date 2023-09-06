package io.iohk.atala.agent.notification

import io.iohk.atala.agent.notification.JsonEventEncoders.*
import io.iohk.atala.agent.notification.WebhookPublisherError.UnexpectedError
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.walletapi.model.ManagedDIDDetail
import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.event.notification.EventNotificationConfig
import io.iohk.atala.event.notification.{Event, EventConsumer, EventNotificationService}
import io.iohk.atala.pollux.core.model.{IssueCredentialRecord, PresentationRecord}
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*
import zio.http.*
import zio.http.model.*
import zio.json.*

class WebhookPublisher(
    appConfig: AppConfig,
    notificationService: EventNotificationService,
    walletService: WalletManagementService
) {

  private val config = appConfig.agent.webhookPublisher

  private val baseHeaders = Headers.contentType(HeaderValues.applicationJson)

  private val parallelism = config.parallelism.getOrElse(1).max(1).min(10)

  val run: ZIO[Client, WebhookPublisherError, Unit] = {
    for {
      connectConsumer <- notificationService
        .consumer[ConnectionRecord]("Connect")
        .mapError(e => UnexpectedError(e.toString))
      issueConsumer <- notificationService
        .consumer[IssueCredentialRecord]("Issue")
        .mapError(e => UnexpectedError(e.toString))
      presentationConsumer <- notificationService
        .consumer[PresentationRecord]("Presentation")
        .mapError(e => UnexpectedError(e.toString))
      didStateConsumer <- notificationService
        .consumer[ManagedDIDDetail]("DIDDetail")
        .mapError(e => UnexpectedError(e.toString))
      _ <- pollAndNotify(connectConsumer).forever.debug.forkDaemon
      _ <- pollAndNotify(issueConsumer).forever.debug.forkDaemon
      _ <- pollAndNotify(presentationConsumer).forever.debug.forkDaemon
      _ <- pollAndNotify(didStateConsumer).forever.debug.forkDaemon
    } yield ()
  }

  private[this] def pollAndNotify[A](consumer: EventConsumer[A])(implicit encoder: JsonEncoder[A]) = {
    for {
      _ <- ZIO.log(s"Polling $parallelism event(s)")
      events <- consumer.poll(parallelism).mapError(e => UnexpectedError(e.toString))
      _ <- ZIO.log(s"Got ${events.size} event(s)")
      allWebhookUrls <- ZIO
        .foreach(events.map(_.walletId).toSet.toList) { walletId =>
          walletService.listWalletNotifications
            .map(walletId -> _)
            .provide(ZLayer.succeed(WalletAccessContext(walletId)))
        }
        .map(_.toMap)
      _ <- ZIO.foreachPar(events) { e =>
        val webhookUrls = allWebhookUrls.getOrElse(e.walletId, Nil)
        ZIO.foreach(webhookUrls) { webhookUrl =>
          notifyWebhook(e, webhookUrl)
            .retry(Schedule.spaced(5.second) && Schedule.recurs(2))
            .catchAll(e => ZIO.logError(s"Webhook permanently failing, with last error being: ${e.msg}"))
        }
      }
    } yield ()
  }

  private[this] def notifyWebhook[A](event: Event[A], conf: EventNotificationConfig)(implicit
      encoder: JsonEncoder[A]
  ): ZIO[Client, UnexpectedError, Unit] = {
    val customHeaders = conf.customHeaders.foldLeft(Headers.empty) { case (acc, (k, v)) => acc ++ Header(k, v) }
    for {
      _ <- ZIO.logDebug(s"Sending event: $event to HTTP webhook URL: ${conf.url}.")
      response <- Client
        .request(
          url = conf.url.toString,
          method = Method.POST,
          headers = baseHeaders ++ customHeaders,
          content = Body.fromString(event.toJson)
        )
        .timeoutFail(new RuntimeException("Client request timed out"))(5.seconds)
        .mapError(t => UnexpectedError(s"Webhook request error: $t"))
      resp <- response match
        case Response(status, _, _, _, _) if status.isSuccess =>
          ZIO.unit
        case Response(status, _, _, _, maybeHttpError) =>
          ZIO.fail(
            UnexpectedError(
              s"Unsuccessful webhook response: [status: $status] [error: ${maybeHttpError.getOrElse("none")}]"
            )
          )
    } yield resp
  }
}

object WebhookPublisher {
  val layer: URLayer[AppConfig & EventNotificationService & WalletManagementService, WebhookPublisher] =
    ZLayer.fromFunction(WebhookPublisher(_, _, _))
}
