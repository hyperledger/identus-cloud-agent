package io.iohk.atala.issue.controller

import io.iohk.atala.agent.server.ControllerHelper
import io.iohk.atala.api.http.model.CollectionStats
import io.iohk.atala.api.http.model.PaginationInput
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.api.util.PaginationUtils
import io.iohk.atala.connect.controller.ConnectionController
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.issue.controller.IssueController.toHttpError
import io.iohk.atala.issue.controller.http.{
  AcceptCredentialOfferRequest,
  CreateIssueCredentialRecordRequest,
  IssueCredentialRecord,
  IssueCredentialRecordPage
}
import io.iohk.atala.pollux.core.model.DidCommID
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.service.CredentialService
import zio.{IO, URLayer, ZIO, ZLayer}

class IssueControllerImpl(
    credentialService: CredentialService,
    connectionService: ConnectionService
) extends IssueController
    with ControllerHelper {
  override def createCredentialOffer(
      request: CreateIssueCredentialRecordRequest
  )(implicit rc: RequestContext): IO[ErrorResponse, IssueCredentialRecord] = {
    val result: IO[ConnectionServiceError | CredentialServiceError | ErrorResponse, IssueCredentialRecord] = for {
      didIdPair <- getPairwiseDIDs(request.connectionId).provide(ZLayer.succeed(connectionService))
      issuingDID <- extractPrismDIDFromString(request.issuingDID)
      jsonClaims <- ZIO
        .fromEither(io.circe.parser.parse(request.claims.toString()))
        .mapError(e => ErrorResponse.badRequest(detail = Some(e.getMessage)))
      outcome <- credentialService
        .createIssueCredentialRecord(
          pairwiseIssuerDID = didIdPair.myDID,
          pairwiseHolderDID = didIdPair.theirDid,
          thid = DidCommID(),
          maybeSchemaId = request.schemaId,
          claims = jsonClaims,
          validityPeriod = request.validityPeriod,
          automaticIssuance = request.automaticIssuance.orElse(Some(true)),
          awaitConfirmation = Some(false),
          issuingDID = Some(issuingDID.asCanonical)
        )
    } yield IssueCredentialRecord.fromDomain(outcome)
    mapIssueErrors(result)
  }

  override def getCredentialRecords(paginationInput: PaginationInput, thid: Option[String])(implicit
      rc: RequestContext
  ): IO[ErrorResponse, IssueCredentialRecordPage] = {
    val uri = rc.request.uri
    val pagination = paginationInput.toPagination
    val result = for {
      pageResult <- thid match
        case None =>
          credentialService
            .getIssueCredentialRecords(offset = Some(pagination.offset), limit = Some(pagination.limit))
            .map(_ -> 1000) // TODO: use actual count
        case Some(thid) =>
          credentialService
            .getIssueCredentialRecordByThreadId(DidCommID(thid))
            .map(_.toSeq)
            .map(records => records -> records.length)
      (records, totalCount) = pageResult
      stats = CollectionStats(totalCount = totalCount, filteredCount = totalCount)
    } yield IssueCredentialRecordPage(
      self = uri.toString(),
      kind = "Collection",
      pageOf = PaginationUtils.composePageOfUri(uri).toString,
      next = PaginationUtils.composeNextUri(uri, records, pagination, stats).map(_.toString),
      previous = PaginationUtils.composePreviousUri(uri, records, pagination, stats).map(_.toString),
      contents = (records map IssueCredentialRecord.fromDomain)
    )
    mapIssueErrors(result)
  }

  override def getCredentialRecord(
      recordId: String
  )(implicit rc: RequestContext): IO[ErrorResponse, IssueCredentialRecord] = {
    val result: IO[CredentialServiceError | ErrorResponse, Option[IssueCredentialRecord]] = for {
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
    val result: IO[CredentialServiceError | ErrorResponse, IssueCredentialRecord] = for {
      id <- extractDidCommIdFromString(recordId)
      outcome <- credentialService.acceptCredentialOffer(id, request.subjectId)
    } yield IssueCredentialRecord.fromDomain(outcome)
    mapIssueErrors(result)
  }

  override def issueCredential(
      recordId: String
  )(implicit rc: RequestContext): IO[ErrorResponse, IssueCredentialRecord] = {
    val result: IO[ErrorResponse | CredentialServiceError, IssueCredentialRecord] = for {
      id <- extractDidCommIdFromString(recordId)
      outcome <- credentialService.acceptCredentialRequest(id)
    } yield IssueCredentialRecord.fromDomain(outcome)
    mapIssueErrors(result)
  }

  private def mapIssueErrors[T](
      result: IO[CredentialServiceError | ConnectionServiceError | ErrorResponse, T]
  ): IO[ErrorResponse, T] = {
    result mapError {
      case e: ErrorResponse                  => e
      case connError: ConnectionServiceError => ConnectionController.toHttpError(connError)
      case credError: CredentialServiceError => toHttpError(credError)
    }
  }

}

object IssueControllerImpl {
  val layer: URLayer[CredentialService & ConnectionService, IssueController] =
    ZLayer.fromFunction(IssueControllerImpl(_, _))
}
