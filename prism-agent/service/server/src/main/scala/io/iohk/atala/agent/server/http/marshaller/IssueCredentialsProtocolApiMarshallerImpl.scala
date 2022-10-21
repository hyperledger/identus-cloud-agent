package io.iohk.atala.agent.server.http.marshaller

import zio.*
import io.iohk.atala.agent.openapi.api.IssueCredentialsProtocolApiMarshaller
import spray.json.RootJsonFormat
import io.iohk.atala.pollux.core.service.CredentialService
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import io.iohk.atala.agent.openapi.model.SendCredentialOfferRequest
import io.iohk.atala.agent.openapi.model.CreateIssueCredentialRecordResponse

object IssueCredentialsProtocolApiMarshallerImpl extends JsonSupport {
  val layer: ULayer[IssueCredentialsProtocolApiMarshaller] = ZLayer.succeed {
    new IssueCredentialsProtocolApiMarshaller {
      implicit def fromEntityUnmarshallerSendCredentialOfferRequest
          : FromEntityUnmarshaller[SendCredentialOfferRequest] =
        summon[RootJsonFormat[SendCredentialOfferRequest]]

      implicit def toEntityMarshallerCreateIssueCredentialRecordResponse
          : ToEntityMarshaller[CreateIssueCredentialRecordResponse] =
        summon[RootJsonFormat[CreateIssueCredentialRecordResponse]]
    }
  }
}
