package io.iohk.atala.issue.controller

import io.iohk.atala.agent.server.ControllerHelper
import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.walletapi.model.PublicationState
import io.iohk.atala.agent.walletapi.model.PublicationState.{Created, PublicationPending, Published}
import io.iohk.atala.agent.walletapi.model.error.GetManagedDIDError
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.api.http.model.{CollectionStats, PaginationInput}
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.api.util.PaginationUtils
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.castor.core.model.error.DIDResolutionError
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.connect.core.model.error.ConnectionServiceError
import io.iohk.atala.connect.core.service.ConnectionService
import io.iohk.atala.issue.controller.IssueController.toHttpError
import io.iohk.atala.issue.controller.http.{
  AcceptCredentialOfferRequest,
  CreateIssueCredentialRecordRequest,
  IssueCredentialRecord,
  IssueCredentialRecordPage
}
import io.iohk.atala.pollux.core.model.CredentialFormat.{AnonCreds, JWT}
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.model.{CredentialFormat, DidCommID}
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.shared.models.WalletAccessContext
import zio.{URLayer, ZIO, ZLayer}

class IssueControllerImpl(
    credentialService: CredentialService,
    connectionService: ConnectionService,
    didService: DIDService,
    managedDIDService: ManagedDIDService,
    appConfig: AppConfig
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
      jsonClaims <- ZIO // TODO Get read of Circe and use zio-json all the way down
        .fromEither(io.circe.parser.parse(request.claims.toString()))
        .mapError(e => ErrorResponse.badRequest(detail = Some(e.getMessage)))
      credentialFormat = request.credentialFormat.map(CredentialFormat.valueOf).getOrElse(CredentialFormat.JWT)
      outcome <-
        credentialFormat match
          case JWT =>
            for {
              issuingDID <- ZIO
                .fromOption(request.issuingDID)
                .mapError(_ => ErrorResponse.badRequest(detail = Some("Missing request parameter: issuingDID")))
                .flatMap(extractPrismDIDFromString)
              _ <- validatePrismDID(issuingDID, allowUnpublished = true)
              record <- credentialService
                .createJWTIssueCredentialRecord(
                  pairwiseIssuerDID = didIdPair.myDID,
                  pairwiseHolderDID = didIdPair.theirDid,
                  thid = DidCommID(),
                  maybeSchemaId = request.schemaId,
                  claims = jsonClaims,
                  validityPeriod = request.validityPeriod,
                  automaticIssuance = request.automaticIssuance.orElse(Some(true)),
                  issuingDID = issuingDID.asCanonical
                )
            } yield record
          case AnonCreds =>
            for {
              credentialDefinitionGUID <- ZIO
                .fromOption(request.credentialDefinitionId)
                .mapError(_ =>
                  ErrorResponse.badRequest(detail = Some("Missing request parameter: credentialDefinitionId"))
                )
              credentialDefinitionId = {
                val publicEndpointUrl = appConfig.agent.httpEndpoint.publicEndpointUrl
                val urlSuffix =
                  s"credential-definition-registry/definitions/${credentialDefinitionGUID.toString}/definition"
                val urlPrefix = if (publicEndpointUrl.endsWith("/")) publicEndpointUrl else publicEndpointUrl + "/"
                s"$urlPrefix$urlSuffix"
              }
              record <- credentialService
                .createAnonCredsIssueCredentialRecord(
                  pairwiseIssuerDID = didIdPair.myDID,
                  pairwiseHolderDID = didIdPair.theirDid,
                  thid = DidCommID(),
                  credentialDefinitionGUID = credentialDefinitionGUID,
                  credentialDefinitionId = credentialDefinitionId,
                  claims = jsonClaims,
                  validityPeriod = request.validityPeriod,
                  automaticIssuance = request.automaticIssuance.orElse(Some(true))
                )
            } yield record
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
      contents = records map IssueCredentialRecord.fromDomain
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
      _ <- request.subjectId match
        case Some(did) => extractPrismDIDFromString(did).flatMap(validatePrismDID(_, true))
        case None      => ZIO.succeed(())
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

    mapIssueErrors(result)
  }

  private def mapIssueErrors[R, T](
      result: ZIO[
        R,
        CredentialServiceError | ConnectionServiceError | GetManagedDIDError | DIDResolutionError | ErrorResponse,
        T
      ]
  ): ZIO[R, ErrorResponse, T] = {
    result mapError {
      case e: ErrorResponse                  => e
      case connError: ConnectionServiceError => connError.asInstanceOf[ErrorResponse] // use implicit conversion
      case credError: CredentialServiceError => toHttpError(credError)
      case resError: DIDResolutionError =>
        ErrorResponse.internalServerError(detail = Some(s"Unable to resolve PrismDID. ${resError.toString()}"))
      case getError: GetManagedDIDError =>
        ErrorResponse.internalServerError(detail = Some(s"Unable to get PrismDID from storage. ${getError.toString()}"))
    }
  }

}

object IssueControllerImpl {
  val layer
      : URLayer[CredentialService & ConnectionService & DIDService & ManagedDIDService & AppConfig, IssueController] =
    ZLayer.fromFunction(IssueControllerImpl(_, _, _, _, _))
}
