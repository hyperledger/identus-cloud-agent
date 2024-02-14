package io.iohk.atala.presentproof.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.pollux.core.service.serdes.*
import io.iohk.atala.pollux.core.service.serdes.anoncreds.{
  NonRevokedIntervalV1,
  PresentationRequestV1,
  RequestedAttributeV1,
  RequestedPredicateV1
}
import io.iohk.atala.presentproof.controller.http.RequestPresentationInput.annotations
import sttp.tapir.Schema.annotations.{description, encodedExample}
import sttp.tapir.{Schema, Validator}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.util.UUID

final case class RequestPresentationInput(
    @description(annotations.connectionId.description)
    @encodedExample(annotations.connectionId.example)
    connectionId: UUID,
    @description(annotations.options.description)
    @encodedExample(annotations.options.example)
    options: Option[Options] = None,
    @description(annotations.proofs.description)
    @encodedExample(annotations.proofs.example)
    proofs: Seq[ProofRequestAux],
    @description(annotations.presentationRequest.description)
    @encodedExample(annotations.presentationRequest.example)
    anoncredPresentationRequest: Option[PresentationRequestV1],
    @description(annotations.credentialFormat.description)
    @encodedExample(annotations.credentialFormat.example)
    credentialFormat: Option[String],
)

object RequestPresentationInput {
  object annotations {
    object connectionId
        extends Annotation[UUID](
          description = "The unique identifier of an established connection between the verifier and the prover.",
          example = UUID.fromString("bc528dc8-69f1-4c5a-a508-5f8019047900")
        )
    object options
        extends Annotation[Option[Options]](
          description = "The options to use when creating the proof presentation request (e.g., domain, challenge).",
          example = None
        )
    object proofs
        extends Annotation[Seq[ProofRequestAux]](
          description =
            "The type of proofs requested in the context of this proof presentation request (e.g., VC schema, trusted issuers, etc.)",
          example = Seq.empty
        )

    object presentationRequest
        extends Annotation[PresentationRequestV1](
          description =
            "The type of proofs requested in the context of this proof presentation request (e.g., VC schema, trusted issuers, etc.)",
          example = PresentationRequestV1(
            requested_attributes = Map(
              "attribute1" -> RequestedAttributeV1(
                "Attribute 1",
                List(
                  Map(
                    "cred_def_id" -> "credential_definition_id_of_attribute1"
                  )
                ),
                Some(
                  NonRevokedIntervalV1(
                    Some(1635734400),
                    Some(1735734400)
                  )
                )
              )
            ),
            requested_predicates = Map(
              "predicate1" ->
                RequestedPredicateV1(
                  "Predicate 1",
                  ">=",
                  18,
                  List(
                    Map(
                      "schema_id" -> "schema_id_of_predicate1"
                    )
                  ),
                  Some(
                    NonRevokedIntervalV1(
                      Some(1635734400),
                      None
                    )
                  )
                )
            ),
            name = "Example Presentation Request",
            nonce = "1234567890",
            version = "1.0",
            non_revoked = None
          )
        )

    object credentialFormat
        extends Annotation[Option[String]](
          description = "The credential format (default to 'JWT')",
          example = Some("JWT"),
          validator = Validator.enumeration(
            List(
              Some("JWT"),
              Some("AnonCreds")
            )
          )
        )
  }

  given encoder: JsonEncoder[RequestPresentationInput] =
    DeriveJsonEncoder.gen[RequestPresentationInput]

  given decoder: JsonDecoder[RequestPresentationInput] =
    DeriveJsonDecoder.gen[RequestPresentationInput]

  import io.iohk.atala.pollux.core.service.serdes.anoncreds.PresentationRequestV1.given

  given Schema[PresentationRequestV1] = Schema.derived

  given Schema[RequestedAttributeV1] = Schema.derived

  given Schema[RequestedPredicateV1] = Schema.derived

  given Schema[NonRevokedIntervalV1] = Schema.derived

  given schema: Schema[RequestPresentationInput] = Schema.derived
}
