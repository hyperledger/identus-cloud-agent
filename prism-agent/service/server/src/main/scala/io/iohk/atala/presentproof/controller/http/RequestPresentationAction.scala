package io.iohk.atala.presentproof.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.presentproof.controller.http.RequestPresentationAction.annotations
import sttp.tapir.Schema.annotations.{description, encodedExample, validate}
import sttp.tapir.{Schema, Validator}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class RequestPresentationAction(
    @description(annotations.action.description)
    @encodedExample(annotations.action.example)
    @validate(annotations.action.validator)
    action: String,
    @description(annotations.proofId.description)
    @encodedExample(annotations.proofId.example)
    proofId: Option[Seq[String]] = None
)

object RequestPresentationAction {
  object annotations {
    object action
        extends Annotation[String](
          description = "The action to perform on the proof presentation record.",
          example = "request-accept",
          validator = Validator.enumeration(
            List(
              "request-accept",
              "request-reject",
              "presentation-accept",
              "presentation-reject"
            )
          )
        )
    object proofId
        extends Annotation[String](
          description =
            "The unique identifier of the issue credential record - and hence VC - to use as the prover accepts the presentation request. Only applicable on the prover side when the action is `request-accept`.",
          example = "0d3a0f8d-852e-42d5-a6f8-2281c4be945c"
        )
  }

  given encoder: JsonEncoder[RequestPresentationAction] =
    DeriveJsonEncoder.gen[RequestPresentationAction]

  given decoder: JsonDecoder[RequestPresentationAction] =
    DeriveJsonDecoder.gen[RequestPresentationAction]

  given schema: Schema[RequestPresentationAction] = Schema.derived
}
