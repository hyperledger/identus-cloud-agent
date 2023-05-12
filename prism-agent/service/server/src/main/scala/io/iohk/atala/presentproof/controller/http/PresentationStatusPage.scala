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
    self: String = "",
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
          description = "",
          example = ""
        )
    object kind
        extends Annotation[String](
          description = "",
          example = ""
        )
    object pageOf
        extends Annotation[String](
          description = "",
          example = ""
        )
    object next
        extends Annotation[String](
          description = "",
          example = ""
        )
    object previous
        extends Annotation[String](
          description = "",
          example = ""
        )
    object contents
        extends Annotation[String](
          description = "",
          example = ""
        )
  }

  given encoder: JsonEncoder[PresentationStatusPage] =
    DeriveJsonEncoder.gen[PresentationStatusPage]

  given decoder: JsonDecoder[PresentationStatusPage] =
    DeriveJsonDecoder.gen[PresentationStatusPage]

  given schema: Schema[PresentationStatusPage] = Schema.derived
}
