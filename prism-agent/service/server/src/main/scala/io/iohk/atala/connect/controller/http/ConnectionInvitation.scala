package io.iohk.atala.connect.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.connect.controller.http.ConnectionInvitation.annotations
import io.iohk.atala.mercury.protocol.invitation.v2.Invitation

import java.util.UUID
import sttp.tapir.Schema.annotations.{description, encodedExample}

case class ConnectionInvitation(
    @description(annotations.id.description)
    @encodedExample(annotations.id.example)
    id: UUID,
    @description(annotations.`type`.description)
    @encodedExample(annotations.`type`.example)
    `type`: String,
    @description(annotations.from.description)
    @encodedExample(annotations.from.example)
    from: String,
    @description(annotations.invitationUrl.description)
    @encodedExample(annotations.invitationUrl.example)
    invitationUrl: String
)

object ConnectionInvitation {

  def fromDomain(domain: Invitation) = ConnectionInvitation(
    id = UUID.fromString(domain.id),
    `type` = domain.`type`,
    from = domain.from.value,
    invitationUrl = s"https://domain.com/path?_oob=${domain.toBase64}"
  )

  object annotations {
    object id
        extends Annotation[UUID](
          description = "",
          example = UUID.fromString("0527aea1-d131-3948-a34d-03af39aba8b4")
        )

    object `type`
        extends Annotation[String](
          description = "",
          example = ""
        )

    object from
        extends Annotation[String](
          description = "",
          example = ""
        )

    object invitationUrl
        extends Annotation[String](
          description = "",
          example = ""
        )
  }

  val Example = ConnectionInvitation(
    id = annotations.id.example,
    `type` = annotations.`type`.example,
    from = annotations.from.example,
    invitationUrl = annotations.invitationUrl.example
  )
}
