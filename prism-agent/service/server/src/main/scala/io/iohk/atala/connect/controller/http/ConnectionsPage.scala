package io.iohk.atala.connect.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.connect.controller.http.ConnectionsPage.annotations
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class ConnectionsPage(
    @description(annotations.contents.description)
    @encodedExample(annotations.contents.example)
    contents: Seq[Connection],
    @description(annotations.kind.description)
    @encodedExample(annotations.kind.example)
    kind: String = "ConnectionsPage",
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
)

object ConnectionsPage {

  val Example = ConnectionsPage(
    contents = annotations.contents.example,
    kind = annotations.kind.example,
    self = annotations.self.example,
    pageOf = annotations.pageOf.example,
    next = Some(annotations.next.example),
    previous = Some(annotations.previous.example)
  )

  object annotations {
    object contents
        extends Annotation[Seq[Connection]](
          description = "",
          example = Seq.empty
        )

    object kind
        extends Annotation[String](
          description = "",
          example = "ConnectionsPage"
        )

    object self
        extends Annotation[String](
          description = "",
          example = "/prism-agent/connections?offset=10&limit=10"
        )

    object pageOf
        extends Annotation[String](
          description = "",
          example = ""
        )

    object next
        extends Annotation[String](
          description = "",
          example = "/prism-agent/connections?offset=20&limit=10"
        )

    object previous
        extends Annotation[String](
          description = "",
          example = "/prism-agent/connections?offset=0&limit=10"
        )
  }

  given encoder: JsonEncoder[ConnectionsPage] =
    DeriveJsonEncoder.gen[ConnectionsPage]

  given decoder: JsonDecoder[ConnectionsPage] =
    DeriveJsonDecoder.gen[ConnectionsPage]

  given schema: Schema[ConnectionsPage] = Schema.derived
}
