package io.iohk.atala.agent.server.http.marshaller

import zio.*
import io.iohk.atala.agent.openapi.api.IssueCredentialsProtocolApiMarshaller
import spray.json.RootJsonFormat
import io.iohk.atala.pollux.core.service.CredentialService
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.iohk.atala.agent.openapi.model.*

object IssueCredentialsProtocolApiMarshallerImpl extends JsonSupport {
  val layer: ULayer[IssueCredentialsProtocolApiMarshaller] = ZLayer.succeed {
    new IssueCredentialsProtocolApiMarshaller {

      implicit def fromEntityUnmarshallerCreateIssueCredentialRecordRequest
          : FromEntityUnmarshaller[CreateIssueCredentialRecordRequest] =
        summon[RootJsonFormat[CreateIssueCredentialRecordRequest]]

      implicit def toEntityMarshallerIssueCredentialRecord: ToEntityMarshaller[IssueCredentialRecord] =
        summon[RootJsonFormat[IssueCredentialRecord]]

      implicit def toEntityMarshallerIssueCredentialRecordPage: ToEntityMarshaller[IssueCredentialRecordPage] =
        summon[RootJsonFormat[IssueCredentialRecordPage]]

      implicit def toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse] =
        summon[RootJsonFormat[ErrorResponse]]

    }
  }
}
