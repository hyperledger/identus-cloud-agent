package org.hyperledger.identus.issue.controller.http

import org.hyperledger.identus.api.http.Annotation
import org.hyperledger.identus.issue.controller.http.CreateIssueCredentialRecordRequest.annotations
import org.hyperledger.identus.shared.models.KeyId
import sttp.tapir.{Schema, Validator}
import sttp.tapir.json.zio.schemaForZioJsonValue
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.util.UUID
import scala.language.implicitConversions

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
    schemaId: Option[String | List[String]] = None,
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
    issuingDID: String,
    @description(annotations.issuingKid.description)
    @encodedExample(annotations.issuingKid.example)
    issuingKid: Option[KeyId],
    @description(annotations.connectionId.description)
    @encodedExample(annotations.connectionId.example)
    connectionId: Option[UUID],
    @description(annotations.goalcode.description)
    @encodedExample(annotations.goalcode.example)
    goalCode: Option[String] = None,
    @description(annotations.goal.description)
    @encodedExample(annotations.goal.example)
    goal: Option[String] = None,
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
          description = """
          |The URL pointing to the JSON schema that will be used for this offer (should be 'http' or 'https').
          |When dereferenced, the returned content should be a JSON schema compliant with the '[Draft 2020-12](https://json-schema.org/draft/2020-12/release-notes)' version of the specification.
          |Note that this parameter only applies when the offer is of type 'JWT'.
          |""".stripMargin,
          example = Some(
            "https://agent-host.com/cloud-agent/schema-registry/schemas/d9569cec-c81e-4779-aa86-0d5994d82676/schema"
          )
        )

    object credentialDefinitionId
        extends Annotation[Option[UUID]](
          description = """
          |The unique identifier (UUID) of the credential definition that will be used for this offer.
          |It should be the identifier of a credential definition that exists in the issuer agent's database.
          |Note that this parameter only applies when the offer is of type 'AnonCreds'.
          |""".stripMargin,
          example = Some(UUID.fromString("d9569cec-c81e-4779-aa86-0d5994d82676"))
        )

    object credentialFormat
        extends Annotation[Option[String]](
          description = "The credential format for this offer (defaults to 'JWT')",
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
          description = """
          |The set of claims that will be included in the issued credential.
          |The JSON object should comply with the schema applicable for this offer (i.e. 'schemaId' or 'credentialDefinitionId').
          |""".stripMargin,
          example = zio.json.ast.Json.Obj(
            "firstname" -> zio.json.ast.Json.Str("Alice"),
            "lastname" -> zio.json.ast.Json.Str("Wonderland"),
          )
        )

    object automaticIssuance
        extends Annotation[Boolean](
          description = """
            |Specifies whether or not the credential should be automatically generated and issued when receiving the `CredentialRequest` from the holder.
            |If set to `false`, a manual approval by the issuer via another API call will be required for the VC to be issued.
            |""".stripMargin,
          example = true
        )

    object issuingDID
        extends Annotation[String](
          description = """
          |The issuer Prism DID by which the verifiable credential will be issued. DID can be short for or long form.
          |""".stripMargin,
          example = "did:prism:3bb0505d13fcb04d28a48234edb27b0d4e6d7e18a81e2c1abab58f3bbc21ce6f"
        )

    object issuingKid
        extends Annotation[Option[String]](
          description = """
          |Specified the key ID (kid) of the DID, it will be used to sign credential.
          |User should specify just the partial identifier of the key. The full id of the kid MUST be "<issuingDID>#<kid>"
          |Note the cryto algorithm used with depend type of the key.
          |""".stripMargin,
          example = Some("kid1") // TODO 20240902 get the defualt name of the key we generete.
        )

    object connectionId
        extends Annotation[Option[UUID]](
          description = """
            |The unique identifier of a DIDComm connection that already exists between the this issuer agent and the holder cloud or edeg agent.
            |It should be the identifier of a connection that exists in the issuer agent's database.
            |This connection will be used to execute the issue credential protocol.
            |Note: connectionId is only required when the offer is from existing connection.
            |connectionId is not required when the offer is from invitation for connectionless issuance.
            |""".stripMargin,
          example = Some(UUID.fromString("d9569cec-c81e-4779-aa86-0d5994d82676"))
        )

    object goalcode
        extends Annotation[Option[String]](
          description = """
            | A self-attested code the receiver may want to display to the user or use in automatically deciding what to do with the out-of-band message.
            | goalcode is optional and can be provided when the offer is from invitation for connectionless issuance.
            |""".stripMargin,
          example = Some("issue-vc")
        )

    object goal
        extends Annotation[Option[String]](
          description = """
          | A self-attested string that the receiver may want to display to the user about the context-specific goal of the out-of-band message.
          | goal is optional and can be provided when the offer is from invitation for connectionless issuance.
          |""".stripMargin,
          example = Some("To issue a Faber College Graduate credential")
        )
  }

  given schemaIdEncoder: JsonEncoder[String | List[String]] =
    JsonEncoder[String]
      .orElseEither(JsonEncoder[List[String]])
      .contramap[String | List[String]] {
        case schemaId: String        => Left(schemaId)
        case schemaIds: List[String] => Right(schemaIds)
      }

  given schemaIdDecoder: JsonDecoder[String | List[String]] =
    JsonDecoder[List[String]]
      .map(schemaId => schemaId: String | List[String])
      .orElse(JsonDecoder[String].map(schemaId => schemaId: String | List[String]))

  given encoder: JsonEncoder[CreateIssueCredentialRecordRequest] =
    DeriveJsonEncoder.gen[CreateIssueCredentialRecordRequest]

  given decoder: JsonDecoder[CreateIssueCredentialRecordRequest] =
    DeriveJsonDecoder.gen[CreateIssueCredentialRecordRequest]

  given schemaJson: Schema[KeyId] = Schema.schemaForString.map[KeyId](v => Some(KeyId(v)))(KeyId.value)

  given schemaId: Schema[String | List[String]] = Schema
    .schemaForEither(Schema.schemaForString, Schema.schemaForArray[String])
    .map[String | List[String]] {
      case Left(value)   => Some(value)
      case Right(values) => Some(values.toList)
    } {
      case value: String        => Left(value)
      case values: List[String] => Right(values.toArray)
    }

  given schema: Schema[CreateIssueCredentialRecordRequest] = Schema.derived

}
