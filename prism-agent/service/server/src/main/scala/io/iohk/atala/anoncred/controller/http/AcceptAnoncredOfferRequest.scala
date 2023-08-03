package io.iohk.atala.anoncred.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.anoncred.controller.http.AcceptAnoncredOfferRequest.annotations
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import sttp.tapir.{Schema, Validator}
import sttp.tapir.Schema.annotations.validate

/** A request to accept a credential offer received from an issuer.
  *
  * @param subjectId
  *   The short-form subject Prism DID to which the verifiable credential should be issued. for example:
  *   ''did:prism:3bb0505d13fcb04d28a48234edb27b0d4e6d7e18a81e2c1abab58f3bbc21ce6f''
  */
final case class AcceptAnoncredOfferRequest(
    @description(annotations.subjectId.description)
    @encodedExample(annotations.subjectId.example)
    subjectId: String
)

object AcceptAnoncredOfferRequest {

  object annotations {
    object subjectId
        extends Annotation[String](
          description = "The short-form subject Prism DID to which the verifiable credential should be issued.",
          example = "did:prism:3bb0505d13fcb04d28a48234edb27b0d4e6d7e18a81e2c1abab58f3bbc21ce6f"
        )

  }

  given encoder: JsonEncoder[AcceptAnoncredOfferRequest] =
    DeriveJsonEncoder.gen[AcceptAnoncredOfferRequest]

  given decoder: JsonDecoder[AcceptAnoncredOfferRequest] =
    DeriveJsonDecoder.gen[AcceptAnoncredOfferRequest]

  given schema: Schema[AcceptAnoncredOfferRequest] = Schema.derived

}
