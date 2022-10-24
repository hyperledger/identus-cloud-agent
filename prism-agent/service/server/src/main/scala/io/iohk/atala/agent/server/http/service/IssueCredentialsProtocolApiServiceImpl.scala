package io.iohk.atala.agent.server.http.service

import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Route
import io.iohk.atala.agent.openapi.api.IssueCredentialsApi
import io.iohk.atala.agent.openapi.api.IssueCredentialsProtocolApiService
import zio.*
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.agent.openapi.model.*
import java.util.UUID
import io.iohk.atala.agent.server.http.model.HttpServiceError
import io.iohk.atala.pollux.core.model.IssueCredentialError
import io.iohk.atala.agent.server.http.model.{HttpServiceError, OASDomainModelHelper, OASErrorModelHelper}

class IssueCredentialsProtocolApiServiceImpl(credentialService: CredentialService)(using runtime: zio.Runtime[Any])
    extends IssueCredentialsProtocolApiService,
      AkkaZioSupport,
      OASDomainModelHelper,
      OASErrorModelHelper {

  override def createCredentialOffer(request: CreateIssueCredentialRecordRequest)(implicit
      toEntityMarshallerCreateIssueCredentialRecordResponse: ToEntityMarshaller[IssueCredentialRecord],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      outcome <- credentialService
        .createCredentialOffer(
          request.subjectId,
          request.schemaId,
          request.claims,
          request.validityPeriod
        )
        .mapError(HttpServiceError.DomainError[IssueCredentialError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.toOAS).either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => createCredentialOffer201(result)
    }
  }

  override def getCredentialRecords()(implicit
      toEntityMarshallerIssueCredentialRecordCollection: ToEntityMarshaller[IssueCredentialRecordCollection]
  ): Route = onZioSuccess(ZIO.unit) { _ =>
    getCredentialRecords200(
      IssueCredentialRecordCollection(None, None, None, None)
    )
  }

  def getCredentialRecord(recordId: String)(implicit
      toEntityMarshallerIssueCredentialRecord: ToEntityMarshaller[IssueCredentialRecord],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = onZioSuccess(ZIO.unit) { _ =>
    getCredentialRecord200(
      IssueCredentialRecord("schemaId", "subject", Some(3600), Map.empty[String, String], UUID.randomUUID(), "")
    )
  }

  def acceptCredentialOffer(recordId: String): Route = onZioSuccess(ZIO.unit) { _ =>
    acceptCredentialOffer200
  }

  def issueCredential(recordId: String): Route = onZioSuccess(ZIO.unit) { _ =>
    issueCredential200
  }

}

object IssueCredentialsProtocolApiServiceImpl {
  val layer: URLayer[CredentialService, IssueCredentialsProtocolApiService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[CredentialService]
    } yield IssueCredentialsProtocolApiServiceImpl(svc)(using rt)
  }
}
