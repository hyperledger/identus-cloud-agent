package io.iohk.atala.agent.notification
import io.iohk.atala.agent.notification.WebhookPublisherError.{InvalidWebhookURL, UnexpectedError}
import io.iohk.atala.agent.server.config.{AppConfig, WebhookPublisherConfig}
import io.iohk.atala.event.notification.{Event, EventNotificationService}
import zio.{IO, UIO, URLayer, ZIO, ZLayer}

import java.net.{URI, URL}

class WebhookPublisher(appConfig: AppConfig, notificationService: EventNotificationService) {

  private val config = appConfig.agent.webhookPublisher

  private val parallelism = config.parallelism match {
    case Some(p) if p < 1  => 1
    case Some(p) if p > 10 => 10
    case Some(p)           => p
    case None              => 1
  }

  val run: IO[WebhookPublisherError, Unit] = config.url match {
    case Some(url) =>
      for {
        url <- ZIO.attempt(URL(url)).mapError(th => InvalidWebhookURL(s"$url [${th.getMessage}]"))
        consumer <- notificationService.subscribe("ALL").mapError(e => UnexpectedError(e.toString))
        pollAndNotify = for {
          _ <- ZIO.log(s"Polling $parallelism event(s)")
          events <- consumer.poll(parallelism).mapError(e => UnexpectedError(e.toString))
          _ <- ZIO.log(s"Got ${events.size} event(s)")
          _ <- ZIO.foreachPar(events)(e => notifyWebhook(e, url))
        } yield ()
        poll <- pollAndNotify.forever
      } yield poll
    case None => ZIO.unit
  }

  private[this] def notifyWebhook(event: Event, url: URL): UIO[Unit] =
    ZIO.log(s"Sending event: $event to webhook URL: $url with API key ${config.apiKey}")
}

object WebhookPublisher {
  val layer: URLayer[AppConfig & EventNotificationService, WebhookPublisher] =
    ZLayer.fromFunction(WebhookPublisher(_, _))
}
