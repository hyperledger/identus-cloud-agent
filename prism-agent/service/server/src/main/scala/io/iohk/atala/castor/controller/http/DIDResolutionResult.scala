package io.iohk.atala.castor.controller.http

import io.iohk.atala.api.http.Annotation
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonEncoder, JsonDecoder}
import io.iohk.atala.castor.controller.http.DIDResolutionResult.annotations

final case class DIDResolutionResult(
    @description(annotations.`@context`.description)
    @encodedExample(annotations.`@context`.example)
    `@context`: String,
    didDocument: Option[DIDDocument] = None,
    didDocumentMetadata: DIDDocumentMetadata,
    didResolutionMetadata: DIDResolutionMetadata
)

object DIDResolutionResult {
  object annotations {
    object `@context`
        extends Annotation[String](
          description = "The JSON-LD context for the DID resolution result.",
          example = "https://w3id.org/did-resolution/v1"
        )
  }

  given encoder: JsonEncoder[DIDResolutionResult] = DeriveJsonEncoder.gen[DIDResolutionResult]
  given decoder: JsonDecoder[DIDResolutionResult] = DeriveJsonDecoder.gen[DIDResolutionResult]
  given schema: Schema[DIDResolutionResult] = Schema.derived
}
