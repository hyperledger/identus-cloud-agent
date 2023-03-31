package io.iohk.atala.connect.controller.http

import io.iohk.atala.api.http.Annotation
import io.iohk.atala.connect.controller.http.Connection.annotations
import io.iohk.atala.connect.core.model
import io.iohk.atala.connect.core.model.ConnectionRecord.Role
import sttp.model.Uri
import sttp.tapir.Schema.annotations.{description, encodedExample}

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

case class Connection(
    @description(annotations.connectionId.description)
    @encodedExample(annotations.connectionId.example)
    connectionId: UUID,
    @description(annotations.label.description)
    @encodedExample(annotations.label.example)
    label: Option[String] = None,
    @description(annotations.myDid.description)
    @encodedExample(annotations.myDid.example)
    myDid: Option[String] = None,
    @description(annotations.theirDid.description)
    @encodedExample(annotations.theirDid.example)
    theirDid: Option[String] = None,
    @description(annotations.role.description)
    @encodedExample(annotations.role.example)
    role: String,
    @description(annotations.state.description)
    @encodedExample(annotations.state.example)
    state: String,
    @description(annotations.invitation.description)
    @encodedExample(annotations.invitation.example)
    invitation: ConnectionInvitation,
    @description(annotations.createdAt.description)
    @encodedExample(annotations.createdAt.example)
    createdAt: OffsetDateTime,
    @description(annotations.updatedAt.description)
    @encodedExample(annotations.updatedAt.example)
    updatedAt: Option[OffsetDateTime] = None,
    @description(annotations.self.description)
    @encodedExample(annotations.self.example)
    self: String = "",
    @description(annotations.kind.description)
    @encodedExample(annotations.kind.example)
    kind: String = "CredentialSchema"
) {
  def withBaseUri(base: Uri) = withSelf(base.addPath(connectionId.toString).toString)
  def withSelf(self: String) = copy(self = self)
}

object Connection {

  def fromDomain(domain: model.ConnectionRecord): Connection =
    Connection(
      connectionId = domain.id,
      label = domain.label,
      myDid = domain.role match
        case Role.Inviter =>
          domain.connectionResponse.map(_.from).orElse(domain.connectionRequest.map(_.to)).map(_.value)
        case Role.Invitee =>
          domain.connectionResponse.map(_.to).orElse(domain.connectionRequest.map(_.from)).map(_.value)
      ,
      theirDid = domain.role match
        case Role.Inviter =>
          domain.connectionResponse.map(_.to).orElse(domain.connectionRequest.map(_.from)).map(_.value)
        case Role.Invitee =>
          domain.connectionResponse.map(_.from).orElse(domain.connectionRequest.map(_.to)).map(_.value)
      ,
      role = domain.role.toString,
      state = domain.protocolState.toString,
      invitation = ConnectionInvitation.fromDomain(domain.invitation),
      createdAt = domain.createdAt.atOffset(ZoneOffset.UTC),
      updatedAt = domain.updatedAt.map(_.atOffset(ZoneOffset.UTC)),
      self = domain.id.toString,
      kind = "Connection",
    )

  object annotations {
    object connectionId
        extends Annotation[UUID](
          description = "",
          example = UUID.fromString("0527aea1-d131-3948-a34d-03af39aba8b4")
        )

    object label
        extends Annotation[String](
          description = "",
          example = ""
        )

    object myDid
        extends Annotation[String](
          description = "",
          example = ""
        )

    object theirDid
        extends Annotation[String](
          description = "",
          example = ""
        )

    object role
        extends Annotation[String](
          description = "",
          example = ""
        )

    object state
        extends Annotation[String](
          description = "",
          example = ""
        )

    object invitation
        extends Annotation[ConnectionInvitation](
          description = "",
          example = ConnectionInvitation.Example
        )

    object createdAt
        extends Annotation[OffsetDateTime](
          description = "",
          example = OffsetDateTime.parse("2022-03-10T12:00:00Z")
        )

    object updatedAt
        extends Annotation[OffsetDateTime](
          description = "",
          example = OffsetDateTime.parse("2022-03-10T12:00:00Z")
        )

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
  }

}
