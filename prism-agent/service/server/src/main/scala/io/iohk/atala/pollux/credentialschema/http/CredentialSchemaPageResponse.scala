package io.iohk.atala.pollux.credentialschema.http

import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import io.iohk.atala.pollux.credentialschema.http.CredentialSchemaPageResponse.annotations
import io.iohk.atala.api.http.Annotation

case class CredentialSchemaPageResponse(
    @description(annotations.contents.description)
    @encodedExample(annotations.contents.example)
    contents: Seq[CredentialSchemaResponse],
    @description(annotations.kind.description)
    @encodedExample(annotations.kind.example)
    kind: String = "CredentialSchemaPage",
    @description(annotations.self.description)
    @encodedExample(annotations.self.example)
    self: String = "",
    @description(annotations.pageOf.description)
    @encodedExample(annotations.pageOf.example)
    pageOf: String = "",
    @description(annotations.next.description)
    @encodedExample(annotations.next.example)
    next: Option[String] = None,
    @description(annotations.previous.description)
    @encodedExample(annotations.previous.example)
    previous: Option[String] = None
) {
  def withSelf(self: String) = copy(self = self)
}

object CredentialSchemaPageResponse {
  given encoder: JsonEncoder[CredentialSchemaPageResponse] =
    DeriveJsonEncoder.gen[CredentialSchemaPageResponse]
  given decoder: JsonDecoder[CredentialSchemaPageResponse] =
    DeriveJsonDecoder.gen[CredentialSchemaPageResponse]
  given schema: Schema[CredentialSchemaPageResponse] = Schema.derived

  val Example = CredentialSchemaPageResponse(
    contents = annotations.contents.example,
    kind = annotations.kind.example,
    self = annotations.self.example,
    pageOf = annotations.pageOf.example,
    next = Some(annotations.next.example),
    previous = Some(annotations.previous.example)
  )

  object annotations {

    object contents
        extends Annotation[Seq[CredentialSchemaResponse]](
          description =
            "A sequence of CredentialSchemaResponse objects representing the list of credential schemas that the API response contains",
          example = Seq.empty
        )

    object kind
        extends Annotation[String](
          description =
            "A string field indicating the type of the API response. In this case, it will always be set to `CredentialSchemaPage`",
          example = "CredentialSchemaPage"
        )

    object self
        extends Annotation[String](
          description = "A string field containing the URL of the current API endpoint",
          example = "/prism-agent/schema-registry/schemas?skip=10&limit=10"
        )

    object pageOf
        extends Annotation[String](
          description = "A string field indicating the type of resource that the contents field contains",
          example = "/prism-agent/schema-registry/schemas"
        )

    object next
        extends Annotation[String](
          description = "An optional string field containing the URL of the next page of results.<br/>" +
            " If the API response does not contain any more pages, this field should be set to None.",
          example = "/prism-agent/schema-registry/schemas?skip=20&limit=10"
        )

    object previous
        extends Annotation[String](
          description = "An optional string field containing the URL of the previous page of results.<br/>" +
            "If the API response is the first page of results, this field should be set to None.",
          example = "/prism-agent/schema-registry/schemas?skip=0&limit=10"
        )
  }
}
