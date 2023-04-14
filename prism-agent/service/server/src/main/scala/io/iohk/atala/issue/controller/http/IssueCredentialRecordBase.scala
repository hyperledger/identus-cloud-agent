package io.iohk.atala.issue.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.pollux.core.model.IssueCredentialRecord as PolluxIssueCredentialRecord
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import io.iohk.atala.mercury.model.AttachmentDescriptor
import io.iohk.atala.mercury.model.Base64

import java.time.{OffsetDateTime, ZoneOffset}

/**
 * A class to represent an incoming request to create a new credential offer.
 *
 * @param schemaId The unique identifier of the schema used for this credential offer. for example: ''null''
 * @param subjectId The identifier (e.g DID) of the subject to which the verifiable credential will be issued. for example: ''did:prism:subjectofverifiablecredentials''
 * @param validityPeriod The validity period in seconds of the verifiable credential that will be issued. for example: ''3600''
 * @param claims The claims that will be associated with the issued verifiable credential. for example: ''null''
 * @param automaticIssuance Specifies whether or not the credential should be automatically generated and issued when receiving the `CredentialRequest` from the holder. If set to `false`, a manual approval by the issuer via API call will be required for the VC to be issued.  for example: ''null''

 * @param issuingDID The issuer DID of the verifiable credential object. for example: ''did:prism:issuerofverifiablecredentials''
 * @param connectionId The unique identifier of a DIDComm connection that already exists between the issuer and the holder, and that will be used to execute the issue credential protocol. for example: ''null''
 */
final case class CreateIssueCredentialRecordRequest(
  @description(CreateIssueCredentialRecordRequest.annotations.schemaId.description)
  @encodedExample(CreateIssueCredentialRecordRequest.annotations.schemaId.example)
  schemaId: Option[String] = None,
  @description(CreateIssueCredentialRecordRequest.annotations.subjectId.description)
  @encodedExample(CreateIssueCredentialRecordRequest.annotations.subjectId.example)
  subjectId: Option[String] = None,
  @description(CreateIssueCredentialRecordRequest.annotations.validityPeriod.description)
  @encodedExample(CreateIssueCredentialRecordRequest.annotations.validityPeriod.example)
  validityPeriod: Option[Double] = None,
  @description(CreateIssueCredentialRecordRequest.annotations.claims.description)
  @encodedExample(CreateIssueCredentialRecordRequest.annotations.claims.example)
  claims: Map[String, String],
  @description(CreateIssueCredentialRecordRequest.annotations.automaticIssuance.description)
  @encodedExample(CreateIssueCredentialRecordRequest.annotations.automaticIssuance.example)
  automaticIssuance: Option[Boolean] = None,
  @description(CreateIssueCredentialRecordRequest.annotations.issuingDID.description)
  @encodedExample(CreateIssueCredentialRecordRequest.annotations.issuingDID.example)
  issuingDID: String,
  @description(CreateIssueCredentialRecordRequest.annotations.connectionId.description)
  @encodedExample(CreateIssueCredentialRecordRequest.annotations.connectionId.example)
  connectionId: String
)

object CreateIssueCredentialRecordRequest {

  object annotations {

    object schemaId
      extends Annotation[String](
        description = "The unique identifier of the schema used for this credential offer.",
        example = "null"
      )

    object subjectId
      extends Annotation[String](
        description = "The identifier (e.g DID) of the subject to which the verifiable credential will be issued.",
        example = "did:prism:subjectofverifiablecredentials"
      )

    object validityPeriod
      extends Annotation[Double](
        description = "The validity period in seconds of the verifiable credential that will be issued.",
        example = 3600
      )

    object claims
      extends Annotation[Map[String, String]](
        description = "The claims that will be associated with the issued verifiable credential.",
        example = Map(
          "firstname" -> "Alice",
          "lastname" -> "Wonderland"
        )
      )

    object automaticIssuance
      extends Annotation[Boolean](
        description = "Specifies whether or not the credential should be automatically generated and issued when receiving the `CredentialRequest` from the holder. If set to `false`, a manual approval by the issuer via API call will be required for the VC to be issued.",
        example = true
      )

    object issuingDID
      extends Annotation[String](
        description = "The issuer DID of the verifiable credential object.",
        example = "did:prism:issuerofverifiablecredentials"
      )

    object connectionId
      extends Annotation[String](
        description = "The unique identifier of a DIDComm connection that already exists between the issuer and the holder, and that will be used to execute the issue credential protocol.",
        example = "null"
      )

  }

}

/**
 * A class to represent an an outgoing response for a created credential offer.
 *
 * @param schemaId The unique identifier of the schema used for this credential offer. for example: ''null''
 * @param subjectId The identifier (e.g DID) of the subject to which the verifiable credential will be issued. for example: ''did:prism:subjectofverifiablecredentials''
 * @param validityPeriod The validity period in seconds of the verifiable credential that will be issued. for example: ''3600''
 * @param claims The claims that will be associated with the issued verifiable credential. for example: ''null''
 * @param automaticIssuance Specifies whether or not the credential should be automatically generated and issued when receiving the `CredentialRequest` from the holder. If set to `false`, a manual approval by the issuer via API call will be required for the VC to be issued.  for example: ''null''
 * @param recordId The unique identifier of the issue credential record. for example: ''null''
 * @param createdAt The date and time when the issue credential record was created. for example: ''null''
 * @param updatedAt The date and time when the issue credential record was last updated. for example: ''null''
 * @param role The role played by the Prism agent in the credential issuance flow. for example: ''null''
 * @param protocolState The current state of the issue credential protocol execution. for example: ''null''
 * @param jwtCredential The base64-encoded JWT verifiable credential that has been sent by the issuer. for example: ''null''
 * @param issuingDID Issuer DID of the verifiable credential object. for example: ''did:prism:issuerofverifiablecredentials''
 */
final case class IssueCredentialRecord(
  @description(IssueCredentialRecord.annotations.schemaId.description)
  @encodedExample(IssueCredentialRecord.annotations.schemaId.example)
  schemaId: Option[String] = None,
  @description(IssueCredentialRecord.annotations.subjectId.description)
  @encodedExample(IssueCredentialRecord.annotations.subjectId.example)
  subjectId: Option[String] = None,
  @description(IssueCredentialRecord.annotations.validityPeriod.description)
  @encodedExample(IssueCredentialRecord.annotations.validityPeriod.example)
  validityPeriod: Option[Double] = None,
  @description(IssueCredentialRecord.annotations.claims.description)
  @encodedExample(IssueCredentialRecord.annotations.claims.example)
  claims: Map[String, String],
  @description(IssueCredentialRecord.annotations.automaticIssuance.description)
  @encodedExample(IssueCredentialRecord.annotations.automaticIssuance.example)
  automaticIssuance: Option[Boolean] = None,
  @description(IssueCredentialRecord.annotations.recordId.description)
  @encodedExample(IssueCredentialRecord.annotations.recordId.example)
  recordId: String,
  @description(IssueCredentialRecord.annotations.createdAt.description)
  @encodedExample(IssueCredentialRecord.annotations.createdAt.example)
  createdAt: OffsetDateTime,
  @description(IssueCredentialRecord.annotations.updatedAt.description)
  @encodedExample(IssueCredentialRecord.annotations.updatedAt.example)
  updatedAt: Option[OffsetDateTime] = None,
  @description(IssueCredentialRecord.annotations.role.description)
  @encodedExample(IssueCredentialRecord.annotations.role.example)
  role: String,
  @description(IssueCredentialRecord.annotations.protocolState.description)
  @encodedExample(IssueCredentialRecord.annotations.protocolState.example)
  protocolState: String,
  @description(IssueCredentialRecord.annotations.jwtCredential.description)
  @encodedExample(IssueCredentialRecord.annotations.jwtCredential.example)
  jwtCredential: Option[String] = None,
  @description(IssueCredentialRecord.annotations.issuingDID.description)
  @encodedExample(IssueCredentialRecord.annotations.issuingDID.example)
  issuingDID: Option[String] = None
)

object IssueCredentialRecord {

  def fromDomain(domain: PolluxIssueCredentialRecord): IssueCredentialRecord =
    IssueCredentialRecord(
      recordId = domain.id.value,
      createdAt = domain.createdAt.atOffset(ZoneOffset.UTC),
      updatedAt = domain.updatedAt.map(_.atOffset(ZoneOffset.UTC)),
      role = domain.role.toString,
      subjectId = domain.subjectId,
      claims = domain.offerCredentialData
        .map(offer => offer.body.credential_preview.attributes.map(attr => (attr.name -> attr.value)).toMap)
        .getOrElse(Map.empty),
      schemaId = domain.schemaId,
      validityPeriod = domain.validityPeriod,
      automaticIssuance = domain.automaticIssuance,
      protocolState = domain.protocolState.toString,
      jwtCredential = domain.issueCredentialData.flatMap(issueCredential => {
        issueCredential.attachments.collectFirst { case AttachmentDescriptor(_, _, Base64(jwt), _, _, _, _) =>
          jwt
        }
      })
    )
  object annotations {

    object schemaId
      extends Annotation[String](
        description = "The unique identifier of the schema used for this credential offer.",
        example = "null"
      )

    object subjectId
      extends Annotation[String](
        description = "The identifier (e.g DID) of the subject to which the verifiable credential will be issued.",
        example = "did:prism:subjectofverifiablecredentials"
      )

    object validityPeriod
      extends Annotation[Double](
        description = "The validity period in seconds of the verifiable credential that will be issued.",
        example = 3600
      )

    object claims
      extends Annotation[Map[String, String]](
        description = "The claims that will be associated with the issued verifiable credential.",
        example = Map(
          "firstname" -> "Alice",
          "lastname" -> "Wonderland"
        )
      )

    object automaticIssuance
      extends Annotation[Boolean](
        description = "Specifies whether or not the credential should be automatically generated and issued when receiving the `CredentialRequest` from the holder. If set to `false`, a manual approval by the issuer via API call will be required for the VC to be issued.",
        example = true
      )

    object recordId
      extends Annotation[String](
        description = "The unique identifier of the issue credential record.",
        example = "80d612dc-0ded-4ac9-90b4-1b8eabb04545"
      )

    object createdAt
      extends Annotation[OffsetDateTime](
        description = "The date and time when the issue credential record was created.",
        example = OffsetDateTime.now()
      )

    object updatedAt
      extends Annotation[Option[OffsetDateTime]](
        description = "The date and time when the issue credential record was last updated.",
        example = None
      )

    object role
      extends Annotation[String](
        description = "The role played by the Prism agent in the credential issuance flow.",
        example = "Issuer"
      )

    object protocolState
      extends Annotation[String]( //TODO Support Enum
        description = "The current state of the issue credential protocol execution.",
        example = "OfferPending"
      )

    object jwtCredential
      extends Annotation[Option[String]](
        description = "The base64-encoded JWT verifiable credential that has been sent by the issuer.",
        example = None
      )

    object issuingDID
      extends Annotation[Option[String]](
        description = "Issuer DID of the verifiable credential object.",
        example = Some("did:prism:issuerofverifiablecredentials")
      )

  }

}
