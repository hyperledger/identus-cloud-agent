package io.iohk.atala.agent.server.http.service

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import io.iohk.atala.agent.openapi.api.IssueCredentialsProtocolApiService
import io.iohk.atala.agent.openapi.model.*
import io.iohk.atala.agent.server.http.model.HttpServiceError
import io.iohk.atala.agent.server.http.model.HttpServiceError.InvalidPayload
import io.iohk.atala.agent.server.http.model.OASDomainModelHelper
import io.iohk.atala.agent.server.http.model.OASErrorModelHelper
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.service.CredentialService
import zio.*

import java.util.UUID
import scala.util.Try
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.agent.server.config.AgentConfig
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.ConnectionRecord.Role
import io.iohk.atala.connect.core.model.ConnectionRecord.ProtocolState

class IssueCredentialsProtocolApiServiceImpl(
    credentialService: CredentialService,
    managedDIDService: ManagedDIDService,
    connectionService: ConnectionService,
    agentConfig: AgentConfig
)(using runtime: zio.Runtime[Any])
    extends IssueCredentialsProtocolApiService,
      AkkaZioSupport,
      OASDomainModelHelper,
      OASErrorModelHelper {

  private[this] case class DidIdPair(myDID: DidId, theirDid: DidId)

  override def createCredentialOffer(request: CreateIssueCredentialRecordRequest)(implicit
      toEntityMarshallerCreateIssueCredentialRecordResponse: ToEntityMarshaller[IssueCredentialRecord],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      didIdPair <- getPairwiseDIDs(request.subjectId)
      outcome <- credentialService
        .createIssueCredentialRecord(
          pairwiseDID = didIdPair.myDID,
          thid = UUID.randomUUID(),
          didIdPair.theirDid.value,
          request.schemaId,
          request.claims,
          request.validityPeriod,
          request.automaticIssuance.orElse(Some(true)),
          request.awaitConfirmation.orElse(Some(false))
        )
        .mapError(HttpServiceError.DomainError[CredentialServiceError].apply)
        .mapError(_.toOAS)
    } yield outcome

    onZioSuccess(result.map(_.toOAS).either) {
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
        .mapError(HttpServiceError.DomainError[CredentialServiceError].apply)
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

  override def getCredentialRecord(recordId: String)(implicit
      toEntityMarshallerIssueCredentialRecord: ToEntityMarshaller[IssueCredentialRecord],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      uuid <- recordId.toUUID
      outcome <- credentialService
        .getIssueCredentialRecord(uuid)
        .mapError(HttpServiceError.DomainError[CredentialServiceError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.map(_.toOAS)).either) {
      case Left(error)         => complete(error.status -> error)
      case Right(Some(result)) => getCredentialRecord200(result)
      case Right(None) => getCredentialRecord404(notFoundErrorResponse(Some("Issue credential record not found")))
    }
  }

  override def acceptCredentialOffer(recordId: String)(implicit
      toEntityMarshallerIssueCredentialRecord: ToEntityMarshaller[IssueCredentialRecord],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      uuid <- recordId.toUUID
      outcome <- credentialService
        .acceptCredentialOffer(uuid)
        .mapError(HttpServiceError.DomainError[CredentialServiceError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.map(_.toOAS)).either) {
      case Left(error)         => complete(error.status -> error)
      case Right(Some(result)) => acceptCredentialOffer200(result)
      case Right(None) => getCredentialRecord404(notFoundErrorResponse(Some("Issue credential record not found")))
    }
  }

  override def issueCredential(recordId: String)(implicit
      toEntityMarshallerIssueCredentialRecord: ToEntityMarshaller[IssueCredentialRecord],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      uuid <- recordId.toUUID
      outcome <- credentialService
        .acceptCredentialRequest(uuid)
        .mapError(HttpServiceError.DomainError[CredentialServiceError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.map(_.toOAS)).either) {
      case Left(error)         => complete(error.status -> error)
      case Right(Some(result)) => issueCredential200(result)
      case Right(None) => getCredentialRecord404(notFoundErrorResponse(Some("Issue credential record not found")))
    }
  }

  private[this] def getPairwiseDIDs(subjectId: String): ZIO[Any, ErrorResponse, DidIdPair] = {
    val didRegex = "^did:.*".r
    subjectId match {
      case didRegex() =>
        for {
          pairwiseDID <- managedDIDService.createAndStorePeerDID(agentConfig.didCommServiceEndpointUrl)
        } yield DidIdPair(pairwiseDID.did, DidId(subjectId))
      case _ =>
        for {
          maybeConnection <- connectionService
            .getConnectionRecord(UUID.fromString(subjectId))
            .mapError(HttpServiceError.DomainError[ConnectionServiceError].apply)
            .mapError(_.toOAS)
          connection <- ZIO
            .fromOption(maybeConnection)
            .mapError(_ => notFoundErrorResponse(Some("Connection not found")))
          connectionResponse <- ZIO
            .fromOption(connection.connectionResponse)
            .mapError(_ => notFoundErrorResponse(Some("ConnectionResponse not found in record")))
          didIdPair <- connection match
            case ConnectionRecord(
                  _,
                  _,
                  _,
                  _,
                  _,
                  Role.Inviter,
                  ProtocolState.ConnectionResponseSent,
                  _,
                  _,
                  Some(resp)
                ) =>
              ZIO.succeed(DidIdPair(resp.from, resp.to))
            case ConnectionRecord(
                  _,
                  _,
                  _,
                  _,
                  _,
                  Role.Invitee,
                  ProtocolState.ConnectionResponseReceived,
                  _,
                  _,
                  Some(resp)
                ) =>
              ZIO.succeed(DidIdPair(resp.to, resp.from))
            case _ =>
              ZIO.fail(badRequestErrorResponse(Some("Invalid connection record state for operation")))
        } yield didIdPair
    }
  }

}

object IssueCredentialsProtocolApiServiceImpl {
  val layer: URLayer[
    CredentialService & ManagedDIDService & ConnectionService & AppConfig,
    IssueCredentialsProtocolApiService
  ] =
    ZLayer.fromZIO {
      for {
        rt <- ZIO.runtime[Any]
        credentialService <- ZIO.service[CredentialService]
        managedDIDService <- ZIO.service[ManagedDIDService]
        connectionService <- ZIO.service[ConnectionService]
        appConfig <- ZIO.service[AppConfig]
      } yield IssueCredentialsProtocolApiServiceImpl(
        credentialService,
        managedDIDService,
        connectionService,
        appConfig.agent
      )(using rt)
    }
}
