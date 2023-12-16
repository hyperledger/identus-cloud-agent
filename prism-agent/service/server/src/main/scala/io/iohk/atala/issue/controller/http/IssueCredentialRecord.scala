package io.iohk.atala.issue.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.issue.controller.http.IssueCredentialRecord.annotations
import io.iohk.atala.mercury.model.{AttachmentDescriptor, Base64}
import io.iohk.atala.pollux.core.model.IssueCredentialRecord as PolluxIssueCredentialRecord
import sttp.tapir.Schema.annotations.{description, encodedExample, validate}
import sttp.tapir.json.zio.schemaForZioJsonValue
import sttp.tapir.{Schema, Validator}
import zio.json.*
import zio.json.ast.Json

import java.nio.charset.StandardCharsets
import java.time.{OffsetDateTime, ZoneOffset}

final case class IssueCredentialRecord(
    @description(annotations.recordId.description)
    @encodedExample(annotations.recordId.example)
    recordId: String,
    @description(annotations.thid.description)
    @encodedExample(annotations.thid.example)
    thid: String,
    @description(annotations.credentialFormat.description)
    @encodedExample(annotations.credentialFormat.example)
    @validate(annotations.credentialFormat.validator)
    credentialFormat: String,
    @description(annotations.subjectId.description)
    @encodedExample(annotations.subjectId.example)
    subjectId: Option[String] = None,
    @description(annotations.validityPeriod.description)
    @encodedExample(annotations.validityPeriod.example)
    validityPeriod: Option[Double] = None,
    @description(annotations.claims.description)
    @encodedExample(annotations.claims.example)
    claims: zio.json.ast.Json,
    @description(annotations.automaticIssuance.description)
    @encodedExample(annotations.automaticIssuance.example)
    automaticIssuance: Option[Boolean] = None,
    @description(annotations.createdAt.description)
    @encodedExample(annotations.createdAt.example)
    createdAt: OffsetDateTime,
    @description(annotations.updatedAt.description)
    @encodedExample(annotations.updatedAt.example)
    updatedAt: Option[OffsetDateTime] = None,
    @description(annotations.role.description)
    @encodedExample(annotations.role.example)
    @validate(annotations.role.validator)
    role: String,
    @description(annotations.protocolState.description)
    @encodedExample(annotations.protocolState.example)
    @validate(annotations.protocolState.validator)
    protocolState: String,
    @description(annotations.credential.description)
    @encodedExample(annotations.credential.example)
    credential: Option[String] = None,
    @description(annotations.issuingDID.description)
    @encodedExample(annotations.issuingDID.example)
    issuingDID: Option[String] = None,
    @description(annotations.metaRetries.description)
    @encodedExample(annotations.metaRetries.example)
    metaRetries: Int
)

object IssueCredentialRecord {

  def fromDomain(domain: PolluxIssueCredentialRecord): IssueCredentialRecord =
    IssueCredentialRecord(
      recordId = domain.id.value,
      thid = domain.thid.value,
      createdAt = domain.createdAt.atOffset(ZoneOffset.UTC),
      updatedAt = domain.updatedAt.map(_.atOffset(ZoneOffset.UTC)),
      role = domain.role.toString,
      credentialFormat = domain.credentialFormat.toString,
      subjectId = domain.subjectId,
      claims = domain.offerCredentialData
        .map(offer =>
          offer.body.credential_preview.body.attributes
            .foldLeft(Json.Obj()) { case (jsObject, attr) =>
              val jsonValue = attr.media_type match
                case Some("application/json") =>
                  val jsonString =
                    String(java.util.Base64.getUrlDecoder.decode(attr.value.getBytes(StandardCharsets.UTF_8)))
                  jsonString.fromJson[Json].getOrElse(Json.Str(s"Unsupported VC claims value: $jsonString"))
                case Some(mime) => Json.Str(s"Unsupported 'media_type': $mime")
                case None       => Json.Str(attr.value)
              jsObject.copy(fields = jsObject.fields.appended(attr.name -> jsonValue))
            }
        )
        .getOrElse(Json.Null),
      validityPeriod = domain.validityPeriod,
      automaticIssuance = domain.automaticIssuance,
      protocolState = domain.protocolState.toString,
      credential = domain.issueCredentialData.flatMap(issueCredential => {
        issueCredential.attachments.collectFirst { case AttachmentDescriptor(_, _, Base64(vc), _, _, _, _, _) =>
          vc
        }
      }),
      metaRetries = domain.metaRetries
    )

  given Conversion[PolluxIssueCredentialRecord, IssueCredentialRecord] = fromDomain

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

    object recordId
        extends Annotation[String](
          description = "The unique identifier of the issue credential record.",
          example = "80d612dc-0ded-4ac9-90b4-1b8eabb04545"
        )

    object thid
        extends Annotation[String](
          description = "The unique identifier of the thread this credential record belongs to. " +
            "The value will identical on both sides of the issue flow (issuer and holder)",
          example = "0527aea1-d131-3948-a34d-03af39aba8b4"
        )

    object credentialFormat
        extends Annotation[String](
          description = "The format used for this credential offer (default to 'JWT')",
          example = "JWT",
          validator = Validator.enumeration(
            List(
              "JWT",
              "AnonCreds"
            )
          )
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
          example = "Issuer",
          validator = Validator.enumeration(
            List(
              "Issuer",
              "Holder"
            )
          )
        )

    object protocolState
        extends Annotation[String](
          description = "The current state of the issue credential protocol execution.",
          example = "OfferPending",
          validator = Validator.enumeration(
            List(
              "OfferPending",
              "OfferSent",
              "OfferReceived",
              "RequestPending",
              "RequestGenerated",
              "RequestSent",
              "RequestReceived",
              "CredentialPending",
              "CredentialGenerated",
              "CredentialSent",
              "CredentialReceived",
              "ProblemReportPending",
              "ProblemReportSent",
              "ProblemReportReceived"
            )
          )
        )

    object credential
        extends Annotation[Option[String]](
          description =
            "The base64-encoded verifiable credential, in JWT or AnonCreds format, that has been sent by the issuer.",
          example = None
        )

    object issuingDID
        extends Annotation[Option[String]](
          description = "Issuer DID of the verifiable credential object.",
          example = Some("did:prism:issuerofverifiablecredentials")
        )

    object metaRetries
        extends Annotation[Int](
          description = "The maximum background processing attempts remaining for this record",
          example = 5
        )

  }

  given encoder: JsonEncoder[IssueCredentialRecord] =
    DeriveJsonEncoder.gen[IssueCredentialRecord]

  given decoder: JsonDecoder[IssueCredentialRecord] =
    DeriveJsonDecoder.gen[IssueCredentialRecord]

  given schema: Schema[IssueCredentialRecord] = Schema.derived

}
