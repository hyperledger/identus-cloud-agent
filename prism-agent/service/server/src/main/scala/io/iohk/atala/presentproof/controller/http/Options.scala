package io.iohk.atala.presentproof.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.presentproof.controller.http.Options.annotations
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class Options(
    @description(annotations.challenge.description)
    @encodedExample(annotations.challenge.example)
    challenge: String,
    @description(annotations.domain.description)
    @encodedExample(annotations.domain.example)
    domain: String
)

object Options {
  object annotations {
    object challenge
        extends Annotation[String](
          description = "",
          example = ""
        )
    object domain
        extends Annotation[String](
          description = "",
          example = ""
        )
  }

  given encoder: JsonEncoder[Options] =
    DeriveJsonEncoder.gen[Options]

  given decoder: JsonDecoder[Options] =
    DeriveJsonDecoder.gen[Options]

  given schema: Schema[Options] = Schema.derived
}
