package io.iohk.atala.presentproof.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.presentproof.controller.http.RequestPresentationAction.annotations
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class RequestPresentationAction(
    @description(annotations.action.description)
    @encodedExample(annotations.action.example)
    action: String,
    @description(annotations.proofId.description)
    @encodedExample(annotations.proofId.example)
    proofId: Option[Seq[String]] = None
)

object RequestPresentationAction {
  object annotations {
    object action
        extends Annotation[String](
          description = "",
          example = ""
        )
    object proofId
        extends Annotation[String](
          description = "",
          example = ""
        )
  }

  given encoder: JsonEncoder[RequestPresentationAction] =
    DeriveJsonEncoder.gen[RequestPresentationAction]

  given decoder: JsonDecoder[RequestPresentationAction] =
    DeriveJsonDecoder.gen[RequestPresentationAction]

  given schema: Schema[RequestPresentationAction] = Schema.derived
}
