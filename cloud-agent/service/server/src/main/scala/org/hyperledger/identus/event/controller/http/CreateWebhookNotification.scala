package org.hyperledger.identus.event.controller.http

import org.hyperledger.identus.api.http.Annotation
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.description
import sttp.tapir.Schema.annotations.encodedExample
import zio.json.DeriveJsonDecoder
import zio.json.DeriveJsonEncoder
import zio.json.JsonDecoder
import zio.json.JsonEncoder

final case class CreateWebhookNotification(
    @description(CreateWebhookNotification.annotations.url.description)
    @encodedExample(CreateWebhookNotification.annotations.url.example)
    url: String,
    customHeaders: Option[Map[String, String]]
)

object CreateWebhookNotification {
  given encoder: JsonEncoder[CreateWebhookNotification] = DeriveJsonEncoder.gen[CreateWebhookNotification]
  given decoder: JsonDecoder[CreateWebhookNotification] = DeriveJsonDecoder.gen[CreateWebhookNotification]
  given schema: Schema[CreateWebhookNotification] = Schema.derived

  object annotations {
    object url
        extends Annotation[String](
          description = "A URL of webhook for event notification",
          example = "http://example.com"
        )
  }

}
