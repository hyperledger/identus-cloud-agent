package io.iohk.atala.agent.server.http.marshaller

import io.circe.Json
import zio._
import io.iohk.atala.agent.openapi.api.ConnectionsManagementApiMarshaller
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.iohk.atala.agent.openapi.model.*
import spray.json.RootJsonFormat

object ConnectionsManagementApiMarshallerImpl extends JsonSupport {
  val layer: ULayer[ConnectionsManagementApiMarshaller] = ZLayer.succeed {
    new ConnectionsManagementApiMarshaller {
      implicit def fromEntityUnmarshallerCreateConnectionRequest: FromEntityUnmarshaller[CreateConnectionRequest] =
        summon[RootJsonFormat[CreateConnectionRequest]]

      implicit def fromEntityUnmarshallerAcceptConnectionInvitationRequest
          : FromEntityUnmarshaller[AcceptConnectionInvitationRequest] =
        summon[RootJsonFormat[AcceptConnectionInvitationRequest]]

      implicit def toEntityMarshallerConnectionCollection: ToEntityMarshaller[ConnectionCollection] =
        summon[RootJsonFormat[ConnectionCollection]]

      implicit def toEntityMarshallerConnection: ToEntityMarshaller[Connection] =
        summon[RootJsonFormat[Connection]]

      implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] =
        summon[RootJsonFormat[ErrorResponse]]
    }
  }
}
