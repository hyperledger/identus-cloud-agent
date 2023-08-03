package io.iohk.atala.anoncred.controller.http

import sttp.tapir.Schema
import io.iohk.atala.anoncred.controller.http.AnoncredRecordPage.annotations
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import io.iohk.atala.api.http.Annotation

/** @param self
  *   The reference to the connection collection itself. for example: ''https://atala-prism-products.io/dids''
  * @param kind
  *   The type of object returned. In this case a `Collection`. for example: ''Collection''
  * @param pageOf
  *   Page number within the context of paginated response. for example: ''null''
  * @param next
  *   URL of the next page (if available) for example: ''null''
  * @param previous
  *   URL of the previous page (if available) for example: ''null''
  * @param contents
  *   for example: ''null''
  */
final case class AnoncredRecordPage(
    @description(annotations.self.description)
    @encodedExample(annotations.self.example)
    self: String,
    @description(annotations.self.description)
    @encodedExample(annotations.self.example)
    kind: String,
    @description(annotations.pageOf.description)
    @encodedExample(annotations.pageOf.example)
    pageOf: String,
    @description(annotations.next.description)
    @encodedExample(annotations.next.example)
    next: Option[String] = None,
    @description(annotations.previous.description)
    @encodedExample(annotations.previous.example)
    previous: Option[String] = None,
    @description(annotations.contents.description)
    @encodedExample(annotations.contents.example)
    contents: Seq[AnoncredRecord] // TODO Tech Debt ticket - deduplicate page response schema
)

object AnoncredRecordPage {

  object annotations {

    object contents
        extends Annotation[Seq[AnoncredRecord]](
          description =
            "A sequence of AnoncredRecord objects representing the list of credential records that the API response contains",
          example = Seq.empty
        )

    object kind
        extends Annotation[String](
          description =
            "A string field indicating the type of the API response. In this case, it will always be set to `Collection`",
          example = "Collection"
        )

    object self
        extends Annotation[String](
          description = "A string field containing the URL of the current API endpoint",
          example =
            "/prism-agent/schema-registry/schemas?skip=10&limit=10" // TODO Tech Debt - make these generic / specific to issue
        )

    object pageOf
        extends Annotation[String](
          description = "A string field indicating the type of resource that the contents field contains",
          example = "/prism-agent/schema-registry/schemas"
        )

    object next
        extends Annotation[String](
          description = "An optional string field containing the URL of the next page of results. " +
            "If the API response does not contain any more pages, this field should be set to None.",
          example = "/prism-agent/schema-registry/schemas?skip=20&limit=10"
        )

    object previous
        extends Annotation[String](
          description = "An optional string field containing the URL of the previous page of results. " +
            "If the API response is the first page of results, this field should be set to None.",
          example = "/prism-agent/schema-registry/schemas?skip=0&limit=10"
        )
  }

  given encoder: JsonEncoder[AnoncredRecordPage] =
    DeriveJsonEncoder.gen[AnoncredRecordPage]

  given decoder: JsonDecoder[AnoncredRecordPage] =
    DeriveJsonDecoder.gen[AnoncredRecordPage]

  given schema: Schema[AnoncredRecordPage] = Schema.derived

}
