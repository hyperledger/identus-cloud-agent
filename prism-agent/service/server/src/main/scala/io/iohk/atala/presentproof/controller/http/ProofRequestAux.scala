package io.iohk.atala.presentproof.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.presentproof.controller.http.ProofRequestAux.annotations
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class ProofRequestAux(
    @description(annotations.schemaId.description)
    @encodedExample(annotations.schemaId.example)
    schemaId: String,
    @description(annotations.trustIssuers.description)
    @encodedExample(annotations.trustIssuers.example)
    trustIssuers: Seq[String]
)

object ProofRequestAux {
  object annotations {
    object schemaId
        extends Annotation[String](
          description = "",
          example = ""
        )
    object trustIssuers
        extends Annotation[String](
          description = "",
          example = ""
        )
  }

  given encoder: JsonEncoder[ProofRequestAux] =
    DeriveJsonEncoder.gen[ProofRequestAux]

  given decoder: JsonDecoder[ProofRequestAux] =
    DeriveJsonDecoder.gen[ProofRequestAux]

  given schema: Schema[ProofRequestAux] = Schema.derived
}
