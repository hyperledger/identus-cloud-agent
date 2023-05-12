package io.iohk.atala.presentproof.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.pollux.core.model.PresentationRecord
import io.iohk.atala.presentproof.controller.http.RequestPresentationOutput.annotations
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class RequestPresentationOutput(
    @description(annotations.presentationId.description)
    @encodedExample(annotations.presentationId.example)
    presentationId: String
)

object RequestPresentationOutput {
  def fromDomain(domain: PresentationRecord): RequestPresentationOutput =
    RequestPresentationOutput(domain.id.value)

  object annotations {
    object presentationId
        extends Annotation[String](
          description = "",
          example = ""
        )
  }

  given encoder: JsonEncoder[RequestPresentationOutput] =
    DeriveJsonEncoder.gen[RequestPresentationOutput]

  given decoder: JsonDecoder[RequestPresentationOutput] =
    DeriveJsonDecoder.gen[RequestPresentationOutput]

  given schema: Schema[RequestPresentationOutput] = Schema.derived
}
