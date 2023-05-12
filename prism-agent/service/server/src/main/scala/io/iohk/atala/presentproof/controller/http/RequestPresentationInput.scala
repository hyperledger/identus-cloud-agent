package io.iohk.atala.presentproof.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.presentproof.controller.http.RequestPresentationInput.annotations
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class RequestPresentationInput(
    @description(annotations.connectionId.description)
    @encodedExample(annotations.connectionId.example)
    connectionId: String,
    @description(annotations.options.description)
    @encodedExample(annotations.options.example)
    options: Option[Options] = None,
    @description(annotations.proofs.description)
    @encodedExample(annotations.proofs.example)
    proofs: Seq[ProofRequestAux]
)

object RequestPresentationInput {
  object annotations {
    object connectionId
        extends Annotation[String](
          description = "",
          example = ""
        )
    object options
        extends Annotation[String](
          description = "",
          example = ""
        )
    object proofs
        extends Annotation[String](
          description = "",
          example = ""
        )
  }

  given encoder: JsonEncoder[RequestPresentationInput] =
    DeriveJsonEncoder.gen[RequestPresentationInput]

  given decoder: JsonDecoder[RequestPresentationInput] =
    DeriveJsonDecoder.gen[RequestPresentationInput]

  given schema: Schema[RequestPresentationInput] = Schema.derived
}
