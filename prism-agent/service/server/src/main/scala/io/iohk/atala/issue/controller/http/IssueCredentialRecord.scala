package io.iohk.atala.issue.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.issue.controller.http.IssueCredentialRecord.annotations
import io.iohk.atala.pollux.core.model.IssueCredentialRecord as PolluxIssueCredentialRecord
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import io.iohk.atala.mercury.model.AttachmentDescriptor
import io.iohk.atala.mercury.model.Base64
import sttp.tapir.Schema

import java.time.{OffsetDateTime, ZoneOffset}

/** A class to represent an an outgoing response for a created credential offer.
  *
  * @param subjectId
  *   The identifier (e.g DID) of the subject to which the verifiable credential will be issued. for example:
  *   ''did:prism:subjectofverifiablecredentials''
  * @param validityPeriod
  *   The validity period in seconds of the verifiable credential that will be issued. for example: ''3600''
  * @param claims
  *   The claims that will be associated with the issued verifiable credential. for example: ''null''
  * @param automaticIssuance
  *   Specifies whether or not the credential should be automatically generated and issued when receiving the
  *   `CredentialRequest` from the holder. If set to `false`, a manual approval by the issuer via API call will be
  *   required for the VC to be issued. for example: ''null''
  * @param recordId
  *   The unique identifier of the issue credential record. for example: ''null''
  * @param createdAt
  *   The date and time when the issue credential record was created. for example: ''null''
  * @param updatedAt
  *   The date and time when the issue credential record was last updated. for example: ''null''
  * @param role
  *   The role played by the Prism agent in the credential issuance flow. for example: ''null''
  * @param protocolState
  *   The current state of the issue credential protocol execution. for example: ''null''
  * @param jwtCredential
  *   The base64-encoded JWT verifiable credential that has been sent by the issuer. for example: ''null''
  * @param issuingDID
  *   Issuer DID of the verifiable credential object. for example: ''did:prism:issuerofverifiablecredentials''
  */
final case class IssueCredentialRecord(
    @description(annotations.subjectId.description)
    @encodedExample(annotations.subjectId.example)
    subjectId: Option[String] = None,
    @description(annotations.validityPeriod.description)
    @encodedExample(annotations.validityPeriod.example)
    validityPeriod: Option[Double] = None,
    @description(annotations.claims.description)
    @encodedExample(annotations.claims.example)
    claims: Map[String, String],
    @description(annotations.automaticIssuance.description)
    @encodedExample(annotations.automaticIssuance.example)
    automaticIssuance: Option[Boolean] = None,
    @description(annotations.recordId.description)
    @encodedExample(annotations.recordId.example)
    recordId: String,
    @description(annotations.createdAt.description)
    @encodedExample(annotations.createdAt.example)
    createdAt: OffsetDateTime,
    @description(annotations.updatedAt.description)
    @encodedExample(annotations.updatedAt.example)
    updatedAt: Option[OffsetDateTime] = None,
    @description(annotations.role.description)
    @encodedExample(annotations.role.example)
    role: String,
    @description(annotations.protocolState.description)
    @encodedExample(annotations.protocolState.example)
    protocolState: String,
    @description(annotations.jwtCredential.description)
    @encodedExample(annotations.jwtCredential.example)
    jwtCredential: Option[String] = None,
    @description(annotations.issuingDID.description)
    @encodedExample(annotations.issuingDID.example)
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
          description =
            "Specifies whether or not the credential should be automatically generated and issued when receiving the `CredentialRequest` from the holder. If set to `false`, a manual approval by the issuer via API call will be required for the VC to be issued.",
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
        extends Annotation[String]( // TODO Support Enum
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

  given encoder: JsonEncoder[IssueCredentialRecord] =
    DeriveJsonEncoder.gen[IssueCredentialRecord]

  given decoder: JsonDecoder[IssueCredentialRecord] =
    DeriveJsonDecoder.gen[IssueCredentialRecord]

  given schema: Schema[IssueCredentialRecord] = Schema.derived

}
