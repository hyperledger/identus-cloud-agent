package io.iohk.atala.presentproof.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.pollux.core.service.serdes.*
import io.iohk.atala.pollux.core.service.serdes.anoncreds.{CredentialProofV1, CredentialProofsV1}
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
    proofId: Option[Seq[String]] = None,
    @description(annotations.anoncredProof.description)
    @encodedExample(annotations.anoncredProof.example)
    anoncredPresentationRequest: Option[CredentialProofsV1],
)

final case class AnoncredProof(
    @description(annotations.credential.description)
    @encodedExample(annotations.credential.example)
    credential: String,
    @description(annotations.requestedAttribute.description)
    @encodedExample(annotations.requestedAttribute.example)
    requestedAttribute: Seq[String],
    @description(annotations.requestedPredicate.description)
    @encodedExample(annotations.requestedPredicate.example)
    requestedPredicate: Seq[String]
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
        extends Annotation[Option[Seq[String]]](
          description =
            "The unique identifier of the issue credential record - and hence VC - to use as the prover accepts the presentation request. Only applicable on the prover side when the action is `request-accept`.",
          example = None
        )

    object anoncredProof
        extends Annotation[Option[Seq[AnoncredProof]]](
          description = "A list of proofs from the Anoncred library, each corresponding to a credential.",
          example = None
        )

    object credential
        extends Annotation[String](
          description =
            "The unique identifier of the issue credential record - and hence VC - to use as the prover accepts the presentation request. Only applicable on the prover side when the action is `request-accept`.",
          example = "id"
        )

    object requestedAttribute
        extends Annotation[Seq[String]](
          description = "The unique identifier of attribute that the credential is expected to provide.",
          example = Seq("Attribute1", "Attribute2")
        )

    object requestedPredicate
        extends Annotation[Seq[String]](
          description = "The unique identifier of Predicate that the credential is expected to answer for.",
          example = Seq("Predicate1", "Predicate2")
        )
  }

  given RequestPresentationActionEncoder: JsonEncoder[RequestPresentationAction] =
    DeriveJsonEncoder.gen[RequestPresentationAction]

  given RequestPresentationActionDecoder: JsonDecoder[RequestPresentationAction] =
    DeriveJsonDecoder.gen[RequestPresentationAction]

  given AnoncredProofEncoder: JsonEncoder[AnoncredProof] =
    DeriveJsonEncoder.gen[AnoncredProof]

  given AnoncredProofDecoder: JsonDecoder[AnoncredProof] =
    DeriveJsonDecoder.gen[AnoncredProof]

  given RequestPresentationActionSchema: Schema[RequestPresentationAction] = Schema.derived

  given AnoncredProofSchema: Schema[AnoncredProof] = Schema.derived

  import io.iohk.atala.pollux.core.service.serdes.anoncreds.CredentialProofsV1.given

  given Schema[CredentialProofsV1] = Schema.derived

  given Schema[CredentialProofV1] = Schema.derived

}
