package io.iohk.atala.issue.controller

import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.server.http.model.HttpServiceError
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.api.http.model.Pagination
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.connect.controller.ConnectionController
import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.ConnectionRecord.{ProtocolState, Role}
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.issue.controller.IssueController.toHttpError
import io.iohk.atala.issue.controller.http.{AcceptCredentialOfferRequest, CreateIssueCredentialRecordRequest, IssueCredentialRecord, IssueCredentialRecordPage}
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.pollux.core.model.DidCommID
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import zio.{IO, URLayer, ZIO, ZLayer}

import java.util.UUID

class IssueControllerImpl(
  credentialService: CredentialService,
  connectionService: ConnectionService,
  appConfig: AppConfig
) extends IssueController {
  override def createCredentialOffer(request: CreateIssueCredentialRecordRequest)(implicit rc: RequestContext): IO[ErrorResponse, IssueCredentialRecord] = {
    val result: IO[String | ConnectionServiceError | CredentialServiceError, IssueCredentialRecord] = for {
      didIdPair <- getPairwiseDIDs(request.connectionId)
      issuingDID <- ZIO.fromEither(PrismDID.fromString(request.issuingDID))
      outcome <- credentialService
        .createIssueCredentialRecord(
          pairwiseIssuerDID = didIdPair.myDID,
          pairwiseHolderDID = didIdPair.theirDid,
          thid = DidCommID(),
          schemaId = request.schemaId,
          claims = request.claims,
          validityPeriod = request.validityPeriod,
          automaticIssuance = request.automaticIssuance.orElse(Some(true)),
          awaitConfirmation = Some(false),
          issuingDID = Some(issuingDID.asCanonical)
        )
    } yield IssueCredentialRecord.fromDomain(outcome) //TODO Optimise this transformation each time we get a list of things

    result.mapError {
      case s: String => toHttpError(CredentialServiceError.UnexpectedError(s))
      case connError: ConnectionServiceError => ConnectionController.toHttpError(connError)
      case credError: CredentialServiceError => toHttpError(credError)
    }
  }

  //TODO - Do not filter this in memory - need to filter at the database level - create tech debt ticket
  //TODO - Implement pagination properly
  override def getCredentialRecords(pagination: Pagination, thid: Option[String])(implicit rc: RequestContext): IO[ErrorResponse, IssueCredentialRecordPage] = {
    val result = for {
      records <- credentialService.getIssueCredentialRecords()
      outcome = thid match
        case None => records
        case Some(value) => records.filter(_.thid.value == value) // this logic should be moved to the DB
    } yield IssueCredentialRecordPage(
      self = "/issue-credentials/records",
      kind = "Collection",
      pageOf = "1",
      next = None,
      previous = None,
      contents = (outcome map IssueCredentialRecord.fromDomain)
    )
    result.mapError(toHttpError)
  }

  override def getCredentialRecord(recordId: String)(implicit rc: RequestContext): IO[ErrorResponse, IssueCredentialRecord] = ???

  override def acceptCredentialOffer(recordId: String, request: AcceptCredentialOfferRequest)(implicit rc: RequestContext): IO[ErrorResponse, IssueCredentialRecord] = ???

  override def issueCredential(recordId: String)(implicit rc: RequestContext): IO[ErrorResponse, IssueCredentialRecord] = ???

  private[this] case class DidIdPair(myDID: DidId, theirDid: DidId)

  private[this] def extractDidIdPairFromValidConnection(connRecord: ConnectionRecord): Option[DidIdPair] = {
    (connRecord.protocolState, connRecord.connectionResponse, connRecord.role) match {
      case (ProtocolState.ConnectionResponseReceived, Some(resp), Role.Invitee) =>
        // If Invitee, myDid is the target
        Some(DidIdPair(resp.to, resp.from))
      case (ProtocolState.ConnectionResponseSent, Some(resp), Role.Inviter) =>
        // If Inviter, myDid is the source
        Some(DidIdPair(resp.from, resp.to))
      case _ => None
    }
  }

  private[this] def getPairwiseDIDs(connectionId: String): IO[ConnectionServiceError, DidIdPair] = {
    val lookupId = UUID.fromString(connectionId)
    for {
      maybeConnection <- connectionService.getConnectionRecord(lookupId)
      didIdPair <- maybeConnection match
        case Some(connRecord: ConnectionRecord) =>
          extractDidIdPairFromValidConnection(connRecord) match {
            case Some(didIdPair: DidIdPair) => ZIO.succeed(didIdPair)
            case None => ZIO.fail(ConnectionServiceError.UnexpectedError("Invalid connection record state for operation"))
          }
        case _ => ZIO.fail(ConnectionServiceError.RecordIdNotFound(lookupId))
    } yield didIdPair
  }

}
