package io.iohk.atala.event.controller.http

import io.iohk.atala.event.notification.EventNotificationConfig
import sttp.tapir.Schema
import zio.json.*

import java.time.Instant
import java.util.UUID
import scala.language.implicitConversions

final case class WebhookNotification(
    id: UUID,
    url: String,
    customHeaders: Map[String, String],
    createdAt: Instant
)

object WebhookNotification {
  given encoder: JsonEncoder[WebhookNotification] = DeriveJsonEncoder.gen[WebhookNotification]
  given decoder: JsonDecoder[WebhookNotification] = DeriveJsonDecoder.gen[WebhookNotification]
  given schema: Schema[WebhookNotification] = Schema.derived

  given Conversion[EventNotificationConfig, WebhookNotification] = notification =>
    WebhookNotification(
      id = notification.id,
      url = notification.url.toString(),
      customHeaders = notification.customHeaders,
      createdAt = notification.createdAt
    )

}

final case class WebhookNotificationPage(
    self: String,
    kind: String = "WebhookNotificationPage",
    pageOf: String,
    next: Option[String] = None,
    previous: Option[String] = None,
    contents: Seq[WebhookNotification]
)

object WebhookNotificationPage {
  given encoder: JsonEncoder[WebhookNotificationPage] = DeriveJsonEncoder.gen[WebhookNotificationPage]
  given decoder: JsonDecoder[WebhookNotificationPage] = DeriveJsonDecoder.gen[WebhookNotificationPage]
  given schema: Schema[WebhookNotificationPage] = Schema.derived
}
