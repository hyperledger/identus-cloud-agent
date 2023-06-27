package io.iohk.atala.agent.notification
import io.iohk.atala.agent.notification.WebhookPublisher.given
import io.iohk.atala.agent.notification.WebhookPublisherError.{InvalidWebhookURL, UnexpectedError}
import io.iohk.atala.agent.server.config.{AppConfig, WebhookPublisherConfig}
import io.iohk.atala.event.notification.EventNotificationServiceError.DecoderError
import io.iohk.atala.event.notification.{Event, EventDecoder, EventNotificationService}
import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import zio.*
import zio.http.*
import zio.http.ZClient.ClientLive
import zio.http.model.{Header, Headers, Method}

import java.net.{URI, URL}

class WebhookPublisher(appConfig: AppConfig, notificationService: EventNotificationService) {

  private val config = appConfig.agent.webhookPublisher
  private val baseHeaders = config.apiKey.map(key => Headers.authorization(key)).getOrElse(Headers.empty)

  private val parallelism = config.parallelism match {
    case Some(p) if p < 1  => 1
    case Some(p) if p > 10 => 10
    case Some(p)           => p
    case None              => 1
  }

  val run: ZIO[Client, WebhookPublisherError, Unit] = config.url match {
    case Some(url) =>
      for {
        url <- ZIO.attempt(URL(url)).mapError(th => InvalidWebhookURL(s"$url [${th.getMessage}]"))
        consumer <- notificationService.consumer("Connect").mapError(e => UnexpectedError(e.toString))
        pollAndNotify = for {
          _ <- ZIO.log(s"Polling $parallelism event(s)")
          events <- consumer.poll(parallelism).mapError(e => UnexpectedError(e.toString))
          _ <- ZIO.log(s"Got ${events.size} event(s)")
          _ <- ZIO.foreachPar(events)(e =>
            notifyWebhook(e, url)
              .retry(Schedule.spaced(5.second) && Schedule.recurs(2))
              .catchAll(e => ZIO.logError(s"Webhook permanently failing, with last error being: ${e.msg}"))
          )
        } yield ()
        poll <- pollAndNotify.forever
      } yield poll
    case None => ZIO.unit
  }

  private[this] def notifyWebhook[A](event: Event[A], url: URL): ZIO[Client, UnexpectedError, Unit] = {
    for {
      _ <- ZIO.log(s"Sending event: $event to HTTP webhook URL: $url with API key ${config.apiKey}")
      response <- Client
        .request(
          url = url.toString,
          method = Method.POST,
          headers = baseHeaders,
          // TODO serialize event to JSON here
          content = Body.fromString(event.data.toString)
        )
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

  given EventDecoder[IssueCredentialRecord] = (data: Any) =>
    ZIO.attempt(data.asInstanceOf[IssueCredentialRecord]).mapError(t => DecoderError(t.getMessage))

  val layer: URLayer[AppConfig & EventNotificationService, WebhookPublisher] =
    ZLayer.fromFunction(WebhookPublisher(_, _))
}
