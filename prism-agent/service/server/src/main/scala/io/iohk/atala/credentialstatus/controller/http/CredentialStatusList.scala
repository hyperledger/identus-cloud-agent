package io.iohk.atala.credentialstatus.controller.http

import io.iohk.atala.pollux.vc.jwt.StatusPurpose
import io.iohk.atala.api.http.Annotation
import sttp.tapir.Schema.annotations.{description, encodedExample}
import io.iohk.atala.credentialstatus.controller.http.CredentialStatusList.annotations
import io.iohk.atala.issue.controller.http.IssueCredentialRecord
import io.iohk.atala.presentproof.controller.http.PresentationStatus
import sttp.tapir.Schema
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder}
import io.iohk.atala.pollux.core.model.CredentialStatusList as CredentialStatusListDomain

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

case class CredentialStatusList(
    @description(annotations.id.description)
    @encodedExample(annotations.id.example)
    id: UUID,
    @description(annotations.issuer.description)
    @encodedExample(annotations.issuer.example)
    issuer: String,
    @description(annotations.issued.description)
    @encodedExample(annotations.issued.example)
    issued: OffsetDateTime,
    @description(annotations.purpose.description)
    @encodedExample(annotations.purpose.example)
    purpose: StatusPurpose,
    @description(annotations.statusListJwtCredential.description)
    @encodedExample(annotations.statusListJwtCredential.example)
    statusListJwtCredential: String,
    @description(annotations.size.description)
    @encodedExample(annotations.size.example)
    size: Int,
    @description(annotations.lastUsedIndex.description)
    @encodedExample(annotations.lastUsedIndex.example)
    lastUsedIndex: Int
)

object CredentialStatusList {

  def fromDomain(domain: CredentialStatusListDomain): CredentialStatusList = CredentialStatusList(
    id = domain.id,
    issuer = domain.issuer.toString,
    issued = domain.issued.atOffset(ZoneOffset.UTC),
    purpose = domain.purpose,
    statusListJwtCredential = domain.statusListJwtCredential,
    size = domain.size,
    lastUsedIndex = domain.lastUsedIndex
  )

  object annotations {
    object id
        extends Annotation[UUID](
          description = "Unique id of the credential status list (as UUID)",
          example = UUID.randomUUID()
        )

    object issuer
        extends Annotation[String](
          description = "Did of the credential issuer, can be used to resolve public key and verify JWT signature",
          example = "did:prism:462c4811bf61d7de25b3baf86c5d2f0609b4debe53792d297bf612269bf8593a"
        )

    object issued
        extends Annotation[OffsetDateTime](
          description = "Date time with time zone offset, indicating when the credential status list was created",
          example = OffsetDateTime.now()
        )

    object purpose
        extends Annotation[StatusPurpose](
          description = "Purpose of status list, can eitehr be Revocation or Suspension",
          example = StatusPurpose.Revocation
        )

    object statusListJwtCredential
        extends Annotation[String](
          description = "Status list credential encoded in a JWT format",
          example =
            "eyJhbGciOiJFUzI1NksifQ.eyJpc3MiOiJkaWQ6cHJpc206NDYyYzQ4MTFiZjYxZDdkZTI1YjNiYWY4NmM1ZDJmMDYwOWI0ZGViZTUzNzkyZDI5N2JmNjEyMjY5YmY4NTkzYSIsInN1YiI6IiIsIm5iZiI6MTcwMjUwNTA0MywidmMiOnsiY3JlZGVudGlhbFN1YmplY3QiOnsic3RhdHVzUHVycG9zZSI6InJldm9jYXRpb24iLCJpZCI6IiIsInR5cGUiOiJTdGF0dXNMaXN0MjAyMSIsImVuY29kZWRMaXN0IjoiSDRzSUFBQUFBQUFBXy0zQk1RRUFBQURDb1BWUGJRd2ZvQUFBQUFBQUFBQUFBQUFBQUFBQUFJQzNBWWJTVktzQVFBQUEifSwidHlwZSI6WyJWZXJpZmlhYmxlQ3JlZGVudGlhbCIsIlN0YXR1c0xpc3QyMDIxQ3JlZGVudGlhbCJdLCJAY29udGV4dCI6WyJodHRwczpcL1wvd3d3LnczLm9yZ1wvMjAxOFwvY3JlZGVudGlhbHNcL3YxIiwiaHR0cHM6XC9cL3czaWQub3JnXC92Y1wvc3RhdHVzLWxpc3RcLzIwMjFcL3YxIl19LCJqdGkiOiJodHRwczpcL1wvZXhhbXBsZS5jb21cL2NyZWRlbnRpYWxzXC9zdGF0dXNcLzA2MGEyYmVjLTZkNmYtNGMxZi05NDE0LWQzYzlkYmQzY2NjOSJ9.5_6zIQXdixoMBCI5uq83hYt7MeI6iXiEi5-Id1ZEFvrWvLJwemub9K6rQWTX0dDQFwky4XHhXncA87nnIigtxw"
        )

    object size
        extends Annotation[Int](
          description = "Total capacity of credential status list bitstring array",
          example = 131072
        )

    object lastUsedIndex
        extends Annotation[Int](
          description = "Last used index in the credential status list bitstring array",
          example = 3
        )
  }

  given encoder: JsonEncoder[CredentialStatusList] =
    DeriveJsonEncoder.gen[CredentialStatusList]

  given StatusPurposeCodec: JsonCodec[StatusPurpose] = JsonCodec[StatusPurpose](
    JsonEncoder[String].contramap[StatusPurpose](_.str),
    JsonDecoder[String].mapOrFail {
      case StatusPurpose.Revocation.str => Right(StatusPurpose.Revocation)
      case StatusPurpose.Suspension.str => Right(StatusPurpose.Suspension)
      case str                          => Left(s"no enum value matched for \"$str\"")
    },
  )

  given decoder: JsonDecoder[CredentialStatusList] =
    DeriveJsonDecoder.gen[CredentialStatusList]

  given statusPurposeSchema: Schema[StatusPurpose] = Schema.derived
  given schema: Schema[CredentialStatusList] = Schema.derived

}
