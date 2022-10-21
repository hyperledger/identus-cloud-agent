package io.iohk.atala.agent.server.http.service

import io.iohk.atala.agent.openapi.api.IssueCredentialsApi
import io.iohk.atala.agent.openapi.api.IssueCredentialsProtocolApiService
import io.iohk.atala.agent.openapi.model.CreateIssueCredentialRecordResponse
import akka.http.scaladsl.server.Route
import io.iohk.atala.agent.openapi.model.SendCredentialOfferRequest
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import zio.*
import io.iohk.atala.pollux.core.service.CredentialService

class IssueCredentialsProtocolApiServiceImpl(credentialService: CredentialService)(using runtime: zio.Runtime[Any])
    extends IssueCredentialsProtocolApiService
    with AkkaZioSupport {

  override def sendCredentialOffer(sendCredentialOfferRequest: SendCredentialOfferRequest)(implicit
      toEntityMarshallerCreateIssueCredentialRecordResponse: ToEntityMarshaller[CreateIssueCredentialRecordResponse]
  ): Route = onZioSuccess(ZIO.unit) { _ => sendCredentialOffer201(CreateIssueCredentialRecordResponse(Some("OK"))) }

}

object IssueCredentialsProtocolApiServiceImpl {
  val layer: URLayer[CredentialService, IssueCredentialsProtocolApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[CredentialService]
    } yield IssueCredentialsProtocolApiServiceImpl(svc)(using rt)
  }
}
