package org.hyperledger.identus.presentproof.controller.http

import org.hyperledger.identus.api.http.Annotation
import org.hyperledger.identus.pollux.core.service.serdes.{
  AnoncredNonRevokedIntervalV1,
  AnoncredPresentationRequestV1,
  AnoncredRequestedAttributeV1,
  AnoncredRequestedPredicateV1
}
import sttp.tapir.{Schema, Validator}
import sttp.tapir.json.zio.*
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import OOBRequestPresentationInput.annotations

//TODO Should I just use RequestPresentationInput and add the optional fields will that cause any confusion
final case class OOBRequestPresentationInput(
    @description(annotations.goalcode.description)
    @encodedExample(annotations.goalcode.example)
    goalCode: Option[String] = None,
    @description(annotations.goal.description)
    @encodedExample(annotations.goal.example)
    goal: Option[String] = None,
    @description(annotations.options.description)
    @encodedExample(annotations.options.example)
    options: Option[Options] = None,
    @description(annotations.proofs.description)
    @encodedExample(annotations.proofs.example)
    proofs: Seq[ProofRequestAux],
    @description(annotations.anoncredPresentationRequest.description)
    @encodedExample(annotations.anoncredPresentationRequest.example)
    anoncredPresentationRequest: Option[AnoncredPresentationRequestV1],
    @description(annotations.claims.description)
    @encodedExample(annotations.claims.example)
    claims: Option[zio.json.ast.Json.Obj],
    @description(annotations.credentialFormat.description)
    @encodedExample(annotations.credentialFormat.example)
    credentialFormat: Option[String],
)

object OOBRequestPresentationInput {
  object annotations {
    object goalcode
        extends Annotation[String](
          description =
            "A self-attested code the receiver may want to display to the user or use in automatically deciding what to do with the out-of-band message.",
          example = "issue-vc"
        )

    object goal
        extends Annotation[String](
          description =
            "A self-attested string that the receiver may want to display to the user about the context-specific goal of the out-of-band message.",
          example = "To issue a Peter College Graduate credential"
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
    object claims
        extends Annotation[Option[zio.json.ast.Json.Obj]](
          description = """
                        |The set of claims to be disclosed from the  issued credential.
                        |The JSON object should comply with the schema applicable for this offer (i.e. 'schemaId' or 'credentialDefinitionId').
                        |""".stripMargin,
          example = Some(
            zio.json.ast.Json.Obj(
              "firstname" -> zio.json.ast.Json.Str("Alice"),
              "lastname" -> zio.json.ast.Json.Str("Wonderland"),
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
              Some("SDJWT"),
              Some("AnonCreds")
            )
          )
        )
  }

  given encoder: JsonEncoder[OOBRequestPresentationInput] =
    DeriveJsonEncoder.gen[OOBRequestPresentationInput]

  given decoder: JsonDecoder[OOBRequestPresentationInput] =
    DeriveJsonDecoder.gen[OOBRequestPresentationInput]

  import AnoncredPresentationRequestV1.given

  given Schema[AnoncredPresentationRequestV1] = Schema.derived

  given Schema[AnoncredRequestedAttributeV1] = Schema.derived

  given Schema[AnoncredRequestedPredicateV1] = Schema.derived

  given Schema[AnoncredNonRevokedIntervalV1] = Schema.derived

  given schema: Schema[OOBRequestPresentationInput] = Schema.derived
}
