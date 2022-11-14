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
import io.iohk.atala.pollux.core.model.error.IssueCredentialError
import io.iohk.atala.agent.server.http.model.{HttpServiceError, OASDomainModelHelper, OASErrorModelHelper}
import scala.util.Try
import io.iohk.atala.agent.server.http.model.HttpServiceError.InvalidPayload

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
        .createIssueCredentialRecord(
          thid = UUID.randomUUID(),
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
      toEntityMarshallerIssueCredentialRecordCollection: ToEntityMarshaller[IssueCredentialRecordCollection],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      outcome <- credentialService
        .getIssueCredentialRecords()
        .mapError(HttpServiceError.DomainError[IssueCredentialError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.map(_.toOAS)).either) {
      case Left(error) => complete(error.status -> error)
      case Right(result) =>
        getCredentialRecords200(
          IssueCredentialRecordCollection(
            items = result,
            offset = 0,
            limit = 0,
            count = result.size
          )
        )
    }
  }

  extension (str: String) {
    def toUUID: ZIO[Any, InvalidPayload, UUID] =
      ZIO
        .fromTry(Try(UUID.fromString(str)))
        .mapError(e => HttpServiceError.InvalidPayload(s"Error parsing string as UUID: ${e.getMessage()}"))
  }

  def getCredentialRecord(recordId: String)(implicit
      toEntityMarshallerIssueCredentialRecord: ToEntityMarshaller[IssueCredentialRecord],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      uuid <- recordId.toUUID
      outcome <- credentialService
        .getIssueCredentialRecord(uuid)
        .mapError(HttpServiceError.DomainError[IssueCredentialError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.map(_.toOAS)).either) {
      case Left(error)         => complete(error.status -> error)
      case Right(Some(result)) => getCredentialRecord200(result)
      case Right(None) => getCredentialRecord404(notFoundErrorResponse(Some("Issue credential record not found")))
    }
  }

  def acceptCredentialOffer(recordId: String)(implicit
      toEntityMarshallerIssueCredentialRecord: ToEntityMarshaller[IssueCredentialRecord],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      uuid <- recordId.toUUID
      outcome <- credentialService
        .acceptCredentialOffer(uuid)
        .mapError(HttpServiceError.DomainError[IssueCredentialError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.map(_.toOAS)).either) {
      case Left(error)         => complete(error.status -> error)
      case Right(Some(result)) => acceptCredentialOffer200(result)
      case Right(None) => getCredentialRecord404(notFoundErrorResponse(Some("Issue credential record not found")))
    }
  }

  def issueCredential(recordId: String)(implicit
      toEntityMarshallerIssueCredentialRecord: ToEntityMarshaller[IssueCredentialRecord],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      uuid <- recordId.toUUID
      outcome <- credentialService
        .acceptCredentialRequest(uuid)
        .mapError(HttpServiceError.DomainError[IssueCredentialError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.map(_.toOAS)).either) {
      case Left(error)         => complete(error.status -> error)
      case Right(Some(result)) => issueCredential200(result)
      case Right(None) => getCredentialRecord404(notFoundErrorResponse(Some("Issue credential record not found")))
    }
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
