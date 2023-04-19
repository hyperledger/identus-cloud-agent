package io.iohk.atala.issue.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.issue.controller.http.AcceptCredentialOfferRequest.annotations
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import sttp.tapir.{Schema, Validator}
import sttp.tapir.Schema.annotations.{description, encodedExample, validate}

/**
 * A request to accept a credential offer received from an issuer.
 *
 * @param subjectId The short-form subject Prism DID to which the verifiable credential should be issued. for example: ''did:prism:3bb0505d13fcb04d28a48234edb27b0d4e6d7e18a81e2c1abab58f3bbc21ce6f''
*/
final case class AcceptCredentialOfferRequest(
  @description(annotations.subjectId.description)
  @encodedExample(annotations.subjectId.example)
  subjectId: String
)

object AcceptCredentialOfferRequest {

  object annotations {
    object subjectId
      extends Annotation[String](
        description = "The short-form subject Prism DID to which the verifiable credential should be issued.",
        example = "did:prism:3bb0505d13fcb04d28a48234edb27b0d4e6d7e18a81e2c1abab58f3bbc21ce6f"
      )

  }

  given encoder: JsonEncoder[AcceptCredentialOfferRequest] =
    DeriveJsonEncoder.gen[AcceptCredentialOfferRequest]

  given decoder: JsonDecoder[AcceptCredentialOfferRequest] =
    DeriveJsonDecoder.gen[AcceptCredentialOfferRequest]

  given schema: Schema[AcceptCredentialOfferRequest] = Schema.derived


}
