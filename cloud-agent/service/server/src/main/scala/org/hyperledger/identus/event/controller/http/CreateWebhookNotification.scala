package org.hyperledger.identus.event.controller.http

import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonEncoder, JsonDecoder}

final case class CreateWebhookNotification(
    url: String,
    customHeaders: Option[Map[String, String]]
)

object CreateWebhookNotification {
  given encoder: JsonEncoder[CreateWebhookNotification] = DeriveJsonEncoder.gen[CreateWebhookNotification]
  given decoder: JsonDecoder[CreateWebhookNotification] = DeriveJsonDecoder.gen[CreateWebhookNotification]
  given schema: Schema[CreateWebhookNotification] = Schema.derived
}
