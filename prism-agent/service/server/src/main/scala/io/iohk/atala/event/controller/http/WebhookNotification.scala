package io.iohk.atala.event.controller.http

import io.iohk.atala.event.notification.EventNotificationConfig
import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonEncoder, JsonDecoder}

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
