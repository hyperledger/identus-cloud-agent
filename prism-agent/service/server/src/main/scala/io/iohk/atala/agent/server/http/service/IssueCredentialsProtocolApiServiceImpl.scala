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
import io.iohk.atala.pollux.core.model.DidCommID
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
import io.iohk.atala.castor.core.model.did.PrismDID

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
      didIdPair <- getPairwiseDIDs(request.connectionId)
      issuingDID <- ZIO
        .fromEither(PrismDID.fromString(request.issuingDID))
        .mapError(HttpServiceError.InvalidPayload.apply)
        .mapError(_.toOAS)
      subjectId <- ZIO
        .fromEither(PrismDID.fromString(request.subjectId))
        .mapError(HttpServiceError.InvalidPayload.apply)
        .mapError(_.toOAS)
      outcome <- credentialService
        .createIssueCredentialRecord(
          pairwiseIssuerDID = didIdPair.myDID,
          pairwiseHolderDID = didIdPair.theirDid,
          thid = DidCommID(),
          subjectId = subjectId.toString,
          schemaId = request.schemaId,
          claims = request.claims,
          validityPeriod = request.validityPeriod,
          automaticIssuance = request.automaticIssuance.orElse(Some(true)),
          awaitConfirmation = Some(false),
          issuingDID = Some(issuingDID.asCanonical)
        )
        .mapError(HttpServiceError.DomainError[CredentialServiceError].apply)
        .mapError(_.toOAS)
    } yield outcome

    onZioSuccess(result.map(_.toOAS).either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => createCredentialOffer201(result)
    }
  }

  override def getCredentialRecords(offset: Option[Int], limit: Option[Int])(implicit
      toEntityMarshallerIssueCredentialRecordCollection: ToEntityMarshaller[IssueCredentialRecordPage],
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
          IssueCredentialRecordPage(
            self = "/issue-credentials/records",
            kind = "Collection",
            pageOf = "1",
            next = None,
            previous = None,
            contents = result
          )
        )
    }
  }

  override def getCredentialRecord(recordId: String)(implicit
      toEntityMarshallerIssueCredentialRecord: ToEntityMarshaller[IssueCredentialRecord],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      id <- recordId.toDidCommID
      outcome <- credentialService
        .getIssueCredentialRecord(id)
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
      id <- recordId.toDidCommID
      outcome <- credentialService
        .acceptCredentialOffer(id)
        .mapError(HttpServiceError.DomainError[CredentialServiceError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.toOAS).either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => acceptCredentialOffer200(result)
      // case Right(None) => getCredentialRecord404(notFoundErrorResponse(Some("Issue credential record not found"))) // TODO this is now Left
    }
  }

  override def issueCredential(recordId: String)(implicit
      toEntityMarshallerIssueCredentialRecord: ToEntityMarshaller[IssueCredentialRecord],
      toEntityMarshallerErrorResponse: ToEntityMarshaller[ErrorResponse]
  ): Route = {
    val result = for {
      id <- recordId.toDidCommID
      outcome <- credentialService
        .acceptCredentialRequest(id)
        .mapError(HttpServiceError.DomainError[CredentialServiceError].apply)
    } yield outcome

    onZioSuccess(result.mapBoth(_.toOAS, _.toOAS).either) {
      case Left(error)   => complete(error.status -> error)
      case Right(result) => issueCredential200(result)
      // case Right(None) => getCredentialRecord404(notFoundErrorResponse(Some("Issue credential record not found"))) // TODO this is now Left
    }
  }

  private[this] def getPairwiseDIDs(connectionId: String): ZIO[Any, ErrorResponse, DidIdPair] = {
    for {
      maybeConnection <- connectionService
        .getConnectionRecord(UUID.fromString(connectionId))
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
              Some(resp),
              _, // metaRetries: Int,
              _, // metaLastFailure: Option[String]
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
              Some(resp),
              _, // metaRetries: Int,
              _, // metaLastFailure: Option[String]
            ) =>
          ZIO.succeed(DidIdPair(resp.to, resp.from))
        case _ =>
          ZIO.fail(badRequestErrorResponse(Some("Invalid connection record state for operation")))
    } yield didIdPair

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
