package org.hyperledger.identus.presentproof.controller.http

import org.hyperledger.identus.api.http.Annotation
import org.hyperledger.identus.pollux.core.service.serdes.*
import org.hyperledger.identus.presentproof.controller.http.RequestPresentationInput.annotations
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
    @description(annotations.anoncredPresentationRequest.description)
    @encodedExample(annotations.anoncredPresentationRequest.example)
    anoncredPresentationRequest: Option[AnoncredPresentationRequestV1],
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

    object anoncredPresentationRequest
        extends Annotation[Option[AnoncredPresentationRequestV1]](
          description = "Anoncred Presentation Request",
          example = Some(
            AnoncredPresentationRequestV1(
              requested_attributes = Map(
                "attribute1" -> AnoncredRequestedAttributeV1(
                  "Attribute 1",
                  List(
                    Map(
                      "cred_def_id" -> "credential_definition_id_of_attribute1"
                    )
                  ),
                  Some(
                    AnoncredNonRevokedIntervalV1(
                      Some(1635734400),
                      Some(1735734400)
                    )
                  )
                )
              ),
              requested_predicates = Map(
                "predicate1" ->
                  AnoncredRequestedPredicateV1(
                    "Predicate 1",
                    ">=",
                    18,
                    List(
                      Map(
                        "schema_id" -> "schema_id_of_predicate1"
                      )
                    ),
                    Some(
                      AnoncredNonRevokedIntervalV1(
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

  import AnoncredPresentationRequestV1.given

  given Schema[AnoncredPresentationRequestV1] = Schema.derived

  given Schema[AnoncredRequestedAttributeV1] = Schema.derived

  given Schema[AnoncredRequestedPredicateV1] = Schema.derived

  given Schema[AnoncredNonRevokedIntervalV1] = Schema.derived

  given schema: Schema[RequestPresentationInput] = Schema.derived
}
