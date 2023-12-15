package io.iohk.atala.issue.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.issue.controller.http.CreateIssueCredentialRecordRequest.annotations
import sttp.tapir.Schema.annotations.{description, encodedExample}
import sttp.tapir.json.zio.schemaForZioJsonValue
import sttp.tapir.{Schema, Validator}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.util.UUID

/** A class to represent an incoming request to create a new credential offer.
  *
  * @param validityPeriod
  *   The validity period in seconds of the verifiable credential that will be issued. for example: ''3600''
  * @param claims
  *   The claims that will be associated with the issued verifiable credential. for example: ''null''
  * @param automaticIssuance
  *   Specifies whether or not the credential should be automatically generated and issued when receiving the
  *   `CredentialRequest` from the holder. If set to `false`, a manual approval by the issuer via API call will be
  *   required for the VC to be issued. for example: ''null''
  *
  * @param issuingDID
  *   The issuer DID of the verifiable credential object. for example: ''did:prism:issuerofverifiablecredentials''
  * @param connectionId
  *   The unique identifier of a DIDComm connection that already exists between the issuer and the holder, and that will
  *   be used to execute the issue credential protocol. for example: ''null''
  */
final case class CreateIssueCredentialRecordRequest(
    @description(annotations.validityPeriod.description)
    @encodedExample(annotations.validityPeriod.example)
    validityPeriod: Option[Double] = None,
    @description(annotations.schemaId.description)
    @encodedExample(annotations.schemaId.example)
    schemaId: Option[String],
    @description(annotations.credentialDefinitionId.description)
    @encodedExample(annotations.credentialDefinitionId.example)
    credentialDefinitionId: Option[UUID],
    @description(annotations.credentialFormat.description)
    @encodedExample(annotations.credentialFormat.example)
    credentialFormat: Option[String],
    @description(annotations.claims.description)
    @encodedExample(annotations.claims.example)
    claims: zio.json.ast.Json,
    @description(annotations.automaticIssuance.description)
    @encodedExample(annotations.automaticIssuance.example)
    automaticIssuance: Option[Boolean] = None,
    @description(annotations.issuingDID.description)
    @encodedExample(annotations.issuingDID.example)
    issuingDID: Option[String],
    @description(annotations.connectionId.description)
    @encodedExample(annotations.connectionId.example)
    connectionId: UUID
)

object CreateIssueCredentialRecordRequest {

  object annotations {

    object validityPeriod
        extends Annotation[Double](
          description = "The validity period in seconds of the verifiable credential that will be issued.",
          example = 3600
        )

    object schemaId
        extends Annotation[Option[String]](
          description = "The unique identifier of the schema used for this credential offer.",
          example =
            Some("https://agent-host.com/prism-agent/schema-registry/schemas/d9569cec-c81e-4779-aa86-0d5994d82676")
        )

    object credentialDefinitionId
        extends Annotation[Option[UUID]](
          description =
            "The unique identifier of the credential definition used for this credential offer (AnonCreds only)",
          example = Some(UUID.fromString("d9569cec-c81e-4779-aa86-0d5994d82676"))
        )

    object credentialFormat
        extends Annotation[Option[String]](
          description = "The format used for this credential offer (default to 'JWT')",
          example = Some("JWT"),
          validator = Validator.enumeration(
            List(
              Some("JWT"),
              Some("AnonCreds")
            )
          )
        )

    object claims
        extends Annotation[zio.json.ast.Json](
          description = "The claims that will be associated with the issued verifiable credential.",
          example = zio.json.ast.Json.Obj(
            "firstname" -> zio.json.ast.Json.Str("Alice"),
            "lastname" -> zio.json.ast.Json.Str("Wonderland"),
          )
        )

    object automaticIssuance
        extends Annotation[Boolean](
          description =
            "Specifies whether or not the credential should be automatically generated and issued when receiving the `CredentialRequest` from the holder. If set to `false`, a manual approval by the issuer via API call will be required for the VC to be issued.",
          example = true
        )

    object issuingDID
        extends Annotation[Option[String]](
          description = "The issuer DID of the verifiable credential (JWT credentials only)",
          example = Some("did:prism:issuerofverifiablecredentials")
        )

    object connectionId
        extends Annotation[UUID](
          description =
            "The unique identifier of a DIDComm connection that already exists between the issuer and the holder, and that will be used to execute the issue credential protocol.",
          example = UUID.fromString("d9569cec-c81e-4779-aa86-0d5994d82676")
        )

  }

  given encoder: JsonEncoder[CreateIssueCredentialRecordRequest] =
    DeriveJsonEncoder.gen[CreateIssueCredentialRecordRequest]

  given decoder: JsonDecoder[CreateIssueCredentialRecordRequest] =
    DeriveJsonDecoder.gen[CreateIssueCredentialRecordRequest]

  given schema: Schema[CreateIssueCredentialRecordRequest] = Schema.derived

}
