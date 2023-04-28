package io.iohk.atala.issue.controller

import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.server.http.model.HttpServiceError
import io.iohk.atala.agent.server.http.model.HttpServiceError.InvalidPayload
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.api.http.model.{Pagination, PaginationInput}
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.connect.controller.ConnectionController
import io.iohk.atala.connect.core.model.ConnectionRecord
import io.iohk.atala.connect.core.model.ConnectionRecord.{ProtocolState, Role}
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.issue.controller.IssueController.toHttpError
import io.iohk.atala.issue.controller.http.{
  AcceptCredentialOfferRequest,
  CreateIssueCredentialRecordRequest,
  IssueCredentialRecord,
  IssueCredentialRecordPage
}
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.pollux.core.model.DidCommID
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import zio.{IO, URLayer, ZIO, ZLayer}

import java.util.UUID
import scala.util.Try

class IssueControllerImpl(
    credentialService: CredentialService,
    connectionService: ConnectionService,
    appConfig: AppConfig
) extends IssueController {
  override def createCredentialOffer(
      request: CreateIssueCredentialRecordRequest
  )(implicit rc: RequestContext): IO[ErrorResponse, IssueCredentialRecord] = {
    val result: IO[ConnectionServiceError | CredentialServiceError | InvalidPayload, IssueCredentialRecord] = for {
      didIdPair <- getPairwiseDIDs(request.connectionId)
      issuingDID <- extractPrismDIDFromString(request.issuingDID)
      outcome <- credentialService
        .createIssueCredentialRecord(
          pairwiseIssuerDID = didIdPair.myDID,
          pairwiseHolderDID = didIdPair.theirDid,
          thid = DidCommID(),
          schemaId = None,
          claims = request.claims,
          validityPeriod = request.validityPeriod,
          automaticIssuance = request.automaticIssuance.orElse(Some(true)),
          awaitConfirmation = Some(false),
          issuingDID = Some(issuingDID.asCanonical)
        )
    } yield IssueCredentialRecord.fromDomain(outcome)
    mapIssueErrors(result)
  }

  // TODO - Tech Debt - Do not filter this in memory - need to filter at the database level
  // TODO - Tech Debt - Implement pagination
  override def getCredentialRecords(paginationInput: PaginationInput, thid: Option[String])(implicit
      rc: RequestContext
  ): IO[ErrorResponse, IssueCredentialRecordPage] = {
    val result = for {
      records <- credentialService.getIssueCredentialRecords()
      outcome = thid match
        case None        => records
        case Some(value) => records.filter(_.thid.value == value) // this logic should be moved to the DB
    } yield IssueCredentialRecordPage(
      self = "/issue-credentials/records",
      kind = "Collection",
      pageOf = "1",
      next = None,
      previous = None,
      contents = (outcome map IssueCredentialRecord.fromDomain) // TODO - Tech Debt - Optimise this transformation - each time we get a list of things we iterate it once here
    )
    mapIssueErrors(result)
  }

  override def getCredentialRecord(
      recordId: String
  )(implicit rc: RequestContext): IO[ErrorResponse, IssueCredentialRecord] = {
    val result: IO[CredentialServiceError | InvalidPayload, Option[IssueCredentialRecord]] = for {
      id <- extractDidCommIdFromString(recordId)
      outcome <- credentialService.getIssueCredentialRecord(id)
    } yield (outcome map IssueCredentialRecord.fromDomain)
    mapIssueErrors(result) someOrFail toHttpError(
      CredentialServiceError.RecordIdNotFound(DidCommID(recordId))
    ) // TODO - Tech Debt - Review if this is safe. Currently is because DidCommID is opaque type => string with no validation
  }

  override def acceptCredentialOffer(recordId: String, request: AcceptCredentialOfferRequest)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, IssueCredentialRecord] = {
    val result: IO[CredentialServiceError | InvalidPayload, IssueCredentialRecord] = for {
      id <- extractDidCommIdFromString(recordId)
      outcome <- credentialService.acceptCredentialOffer(id, request.subjectId)
    } yield IssueCredentialRecord.fromDomain(outcome)
    mapIssueErrors(result)
  }

  override def issueCredential(
      recordId: String
  )(implicit rc: RequestContext): IO[ErrorResponse, IssueCredentialRecord] = {
    val result: IO[InvalidPayload | CredentialServiceError, IssueCredentialRecord] = for {
      id <- extractDidCommIdFromString(recordId)
      outcome <- credentialService.acceptCredentialRequest(id)
    } yield IssueCredentialRecord.fromDomain(outcome)
    mapIssueErrors(result)
  }

  private def mapIssueErrors[T](
      result: IO[CredentialServiceError | ConnectionServiceError | InvalidPayload, T]
  ): IO[ErrorResponse, T] = {
    result mapError {
      case invalidPayload: InvalidPayload =>
        ErrorResponse(
          status = 422,
          `type` = "InvalidPayload",
          title = "error-title",
          detail = Some(invalidPayload.msg),
          instance = "error-instance"
        )
      case connError: ConnectionServiceError =>
        ConnectionController.toHttpError(connError)
      case credError: CredentialServiceError =>
        toHttpError(credError)
    }
  }

  private[this] case class DidIdPair(myDID: DidId, theirDid: DidId)

  private[this] def extractDidCommIdFromString(
      maybeDidCommId: String
  ): IO[InvalidPayload, io.iohk.atala.pollux.core.model.DidCommID] = {
    ZIO
      .fromTry(Try(io.iohk.atala.pollux.core.model.DidCommID(maybeDidCommId)))
      .mapError(e => HttpServiceError.InvalidPayload(s"Error parsing string as DidCommID: ${e.getMessage}"))
  }

  private[this] def extractPrismDIDFromString(maybeDid: String): IO[InvalidPayload, PrismDID] = {
    ZIO
      .fromEither(PrismDID.fromString(maybeDid))
      .mapError(e => HttpServiceError.InvalidPayload(s"Error parsing string as PrismDID: ${e}"))
  }

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
            case None =>
              ZIO.fail(ConnectionServiceError.UnexpectedError("Invalid connection record state for operation"))
          }
        case _ => ZIO.fail(ConnectionServiceError.RecordIdNotFound(lookupId))
    } yield didIdPair
  }

}

object IssueControllerImpl {
  val layer: URLayer[CredentialService & ConnectionService & AppConfig, IssueController] =
    ZLayer.fromFunction(IssueControllerImpl(_, _, _))
}
