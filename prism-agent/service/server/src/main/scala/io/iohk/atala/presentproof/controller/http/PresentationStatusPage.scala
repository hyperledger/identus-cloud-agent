package io.iohk.atala.presentproof.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.presentproof.controller.http.PresentationStatusPage.annotations
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class PresentationStatusPage(
    @description(annotations.contents.description)
    @encodedExample(annotations.contents.example)
    contents: Seq[PresentationStatus],
    @description(annotations.self.description)
    @encodedExample(annotations.self.example)
    self: String = "/present-proof/presentations",
    @description(annotations.kind.description)
    @encodedExample(annotations.kind.example)
    kind: String = "Collection",
    @description(annotations.pageOf.description)
    @encodedExample(annotations.pageOf.example)
    pageOf: String = "1",
    @description(annotations.next.description)
    @encodedExample(annotations.next.example)
    next: Option[String] = None,
    @description(annotations.previous.description)
    @encodedExample(annotations.previous.example)
    previous: Option[String] = None
)

object PresentationStatusPage {
  object annotations {
    object self
        extends Annotation[String](
          description = "The reference to the presentation collection itself.",
          example = "/present-proof/presentations"
        )
    object kind
        extends Annotation[String](
          description = "The type of object returned. In this case a `Collection`.",
          example = "Collection"
        )
    object pageOf
        extends Annotation[String](
          description = "Page number within the context of paginated response.",
          example = "1"
        )
    object next
        extends Annotation[String](
          description = "URL of the next page (if available)",
          example = ""
        )
    object previous
        extends Annotation[String](
          description = "URL of the previous page (if available)",
          example = ""
        )
    object contents
        extends Annotation[Seq[PresentationStatus]](
          description = "A sequence of Presentation objects.",
          example = Seq.empty
        )
  }

  given encoder: JsonEncoder[PresentationStatusPage] =
    DeriveJsonEncoder.gen[PresentationStatusPage]

  given decoder: JsonDecoder[PresentationStatusPage] =
    DeriveJsonDecoder.gen[PresentationStatusPage]

  given schema: Schema[PresentationStatusPage] = Schema.derived
}
