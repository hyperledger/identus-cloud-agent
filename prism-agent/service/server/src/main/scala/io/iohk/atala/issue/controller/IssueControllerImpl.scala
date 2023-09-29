package io.iohk.atala.issue.controller

import io.iohk.atala.agent.server.ControllerHelper
import io.iohk.atala.agent.walletapi.model.PublicationState
import io.iohk.atala.agent.walletapi.model.PublicationState.{Created, PublicationPending, Published}
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.api.http.model.{CollectionStats, PaginationInput}
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.api.util.PaginationUtils
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.castor.core.service.DIDService
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
import io.iohk.atala.shared.models.WalletAccessContext
import zio.{URLayer, ZIO, ZLayer}

class IssueControllerImpl(
    credentialService: CredentialService,
    connectionService: ConnectionService,
    didService: DIDService,
    managedDIDService: ManagedDIDService
) extends IssueController
    with ControllerHelper {
  override def createCredentialOffer(
      request: CreateIssueCredentialRecordRequest
  )(implicit rc: RequestContext): ZIO[WalletAccessContext, ErrorResponse, IssueCredentialRecord] = {
    val result: ZIO[
      WalletAccessContext,
      ConnectionServiceError | CredentialServiceError | ErrorResponse,
      IssueCredentialRecord
    ] = for {
      didIdPair <- getPairwiseDIDs(request.connectionId).provideSomeLayer(ZLayer.succeed(connectionService))
      issuingDID <- extractPrismDIDFromString(request.issuingDID)
      _ <- validatePrismDID(issuingDID, allowUnpublished = true)
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
  ): ZIO[WalletAccessContext, ErrorResponse, IssueCredentialRecordPage] = {
    val uri = rc.request.uri
    val pagination = paginationInput.toPagination
    val result = for {
      pageResult <- thid match
        case None =>
          credentialService
            .getIssueCredentialRecords(
              ignoreWithZeroRetries = false,
              offset = Some(pagination.offset),
              limit = Some(pagination.limit)
            )
        case Some(thid) =>
          credentialService
            .getIssueCredentialRecordByThreadId(DidCommID(thid), ignoreWithZeroRetries = false)
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
  )(implicit rc: RequestContext): ZIO[WalletAccessContext, ErrorResponse, IssueCredentialRecord] = {
    val result: ZIO[WalletAccessContext, CredentialServiceError | ErrorResponse, Option[IssueCredentialRecord]] = for {
      id <- extractDidCommIdFromString(recordId)
      outcome <- credentialService.getIssueCredentialRecord(id)
    } yield (outcome map IssueCredentialRecord.fromDomain)
    mapIssueErrors(result) someOrFail toHttpError(
      CredentialServiceError.RecordIdNotFound(DidCommID(recordId))
    ) // TODO - Tech Debt - Review if this is safe. Currently is because DidCommID is opaque type => string with no validation
  }

  override def acceptCredentialOffer(recordId: String, request: AcceptCredentialOfferRequest)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, IssueCredentialRecord] = {
    val result: ZIO[WalletAccessContext, CredentialServiceError | ErrorResponse, IssueCredentialRecord] = for {
      subjectDID <- extractPrismDIDFromString(request.subjectId)
      _ <- validatePrismDID(subjectDID, allowUnpublished = true)
      id <- extractDidCommIdFromString(recordId)
      outcome <- credentialService.acceptCredentialOffer(id, request.subjectId)
    } yield IssueCredentialRecord.fromDomain(outcome)
    mapIssueErrors(result)
  }

  override def issueCredential(
      recordId: String
  )(implicit rc: RequestContext): ZIO[WalletAccessContext, ErrorResponse, IssueCredentialRecord] = {
    val result: ZIO[WalletAccessContext, ErrorResponse | CredentialServiceError, IssueCredentialRecord] = for {
      id <- extractDidCommIdFromString(recordId)
      outcome <- credentialService.acceptCredentialRequest(id)
    } yield IssueCredentialRecord.fromDomain(outcome)
    mapIssueErrors(result)
  }

  private def validatePrismDID(
      prismDID: PrismDID,
      allowUnpublished: Boolean
  ): ZIO[WalletAccessContext, ErrorResponse, Unit] = {
    val result = for {
      maybeDIDState <- managedDIDService.getManagedDIDState(prismDID.asCanonical)
      maybeMetadata <- didService.resolveDID(prismDID).map(_.map(_._1))
      result <- (maybeDIDState.map(_.publicationState), maybeMetadata.map(_.deactivated)) match {
        case (None, _) =>
          ZIO.fail(ErrorResponse.badRequest(detail = Some("The provided DID can't be found in the agent wallet")))

        case (Some(Created() | PublicationPending(_)), _) if allowUnpublished =>
          ZIO.succeed(())

        case (Some(Created() | PublicationPending(_)), _) =>
          ZIO.fail(ErrorResponse.badRequest(detail = Some("The provided DID is not published")))

        case (Some(Published(_)), None) =>
          ZIO.succeed(())

        case (Some(Published(_)), Some(true)) =>
          ZIO.fail(ErrorResponse.badRequest(detail = Some("The provided DID is published but deactivated")))

        case (Some(Published(_)), Some(false)) =>
          ZIO.succeed(())
      }
    } yield result
    result.mapError {
      case e: ErrorResponse => e
      case _ =>
        ErrorResponse.internalServerError(detail = Some(s"Unexpected error while loading the PrismDID: $prismDID"))
    }
  }

  private def mapIssueErrors[R, T](
      result: ZIO[R, CredentialServiceError | ConnectionServiceError | ErrorResponse, T]
  ): ZIO[R, ErrorResponse, T] = {
    result mapError {
      case e: ErrorResponse                  => e
      case connError: ConnectionServiceError => ConnectionController.toHttpError(connError)
      case credError: CredentialServiceError => toHttpError(credError)
    }
  }

}

object IssueControllerImpl {
  val layer: URLayer[CredentialService & ConnectionService & DIDService & ManagedDIDService, IssueController] =
    ZLayer.fromFunction(IssueControllerImpl(_, _, _, _))
}
