package org.hyperledger.identus.issue.controller

import io.lemonlabs.uri.Url
import org.hyperledger.identus.agent.server.config.AppConfig
import org.hyperledger.identus.agent.server.ControllerHelper
import org.hyperledger.identus.agent.walletapi.model.PublicationState
import org.hyperledger.identus.agent.walletapi.model.PublicationState.{Created, PublicationPending, Published}
import org.hyperledger.identus.agent.walletapi.service.ManagedDIDService
import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
import org.hyperledger.identus.api.http.model.{CollectionStats, PaginationInput}
import org.hyperledger.identus.api.util.PaginationUtils
import org.hyperledger.identus.castor.core.model.did.{PrismDID, VerificationRelationship}
import org.hyperledger.identus.castor.core.service.DIDService
import org.hyperledger.identus.connect.core.service.ConnectionService
import org.hyperledger.identus.issue.controller.http.{
  AcceptCredentialOfferRequest,
  CreateIssueCredentialRecordRequest,
  IssueCredentialRecord,
  IssueCredentialRecordPage
}
import org.hyperledger.identus.pollux.core.model.{CredentialFormat, DidCommID}
import org.hyperledger.identus.pollux.core.model.CredentialFormat.{AnonCreds, JWT, SDJWT}
import org.hyperledger.identus.pollux.core.model.IssueCredentialRecord.Role
import org.hyperledger.identus.pollux.core.service.CredentialService
import org.hyperledger.identus.shared.models.{KeyId, WalletAccessContext}
import zio.{URLayer, ZIO, ZLayer}

import scala.language.implicitConversions

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

    def getIssuingDidFromRequest(request: CreateIssueCredentialRecordRequest) = extractPrismDIDFromString(
      request.issuingDID
    )

    for {
      didIdPair <- getPairwiseDIDs(request.connectionId).provideSomeLayer(ZLayer.succeed(connectionService))
      jsonClaims <- ZIO // TODO: Get read of Circe and use zio-json all the way down
        .fromEither(io.circe.parser.parse(request.claims.toString()))
        .mapError(e => ErrorResponse.badRequest(detail = Some(e.getMessage)))
      credentialFormat = request.credentialFormat.map(CredentialFormat.valueOf).getOrElse(CredentialFormat.JWT)
      outcome <-
        credentialFormat match
          case JWT =>
            for {
              issuingDID <- getIssuingDidFromRequest(request)
              _ <- validatePrismDID(issuingDID, allowUnpublished = true, Role.Issuer)
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
          case SDJWT =>
            for {
              issuingDID <- getIssuingDidFromRequest(request)
              _ <- validatePrismDID(issuingDID, allowUnpublished = true, Role.Issuer)
              record <- credentialService
                .createSDJWTIssueCredentialRecord(
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
              issuingDID <- getIssuingDidFromRequest(request)
              credentialDefinitionGUID <- ZIO
                .fromOption(request.credentialDefinitionId)
                .mapError(_ =>
                  ErrorResponse.badRequest(detail = Some("Missing request parameter: credentialDefinitionId"))
                )
              credentialDefinitionId = {

                val publicEndpointServiceName = appConfig.agent.httpEndpoint.serviceName
                val resourcePath =
                  s"credential-definition-registry/definitions/${credentialDefinitionGUID.toString}/definition"
                val didUrl = Url
                  .parse(s"$issuingDID?resourceService=$publicEndpointServiceName&resourcePath=$resourcePath")
                  .toString

                didUrl
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
  }

  override def getCredentialRecords(paginationInput: PaginationInput, thid: Option[String])(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, IssueCredentialRecordPage] = {
    val uri = rc.request.uri
    val pagination = paginationInput.toPagination
    for {
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
  }

  override def getCredentialRecord(
      recordId: String
  )(implicit rc: RequestContext): ZIO[WalletAccessContext, ErrorResponse, IssueCredentialRecord] = {
    for {
      id <- extractDidCommIdFromString(recordId)
      outcome <- credentialService.getById(id)
    } yield IssueCredentialRecord.fromDomain(outcome)
  }

  override def acceptCredentialOffer(recordId: String, request: AcceptCredentialOfferRequest)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, IssueCredentialRecord] = {
    for {
      _ <- request.subjectId match
        case Some(did) => extractPrismDIDFromString(did).flatMap(validatePrismDID(_, true, Role.Holder))
        case None      => ZIO.succeed(())
      id <- extractDidCommIdFromString(recordId)
      outcome <- credentialService.acceptCredentialOffer(id, request.subjectId, request.keyId.map(KeyId(_)))
    } yield IssueCredentialRecord.fromDomain(outcome)
  }

  override def issueCredential(
      recordId: String
  )(implicit rc: RequestContext): ZIO[WalletAccessContext, ErrorResponse, IssueCredentialRecord] = {
    for {
      id <- extractDidCommIdFromString(recordId)
      outcome <- credentialService.acceptCredentialRequest(id)
    } yield IssueCredentialRecord.fromDomain(outcome)
  }

  private def validatePrismDID(
      prismDID: PrismDID,
      allowUnpublished: Boolean,
      role: Role
  ): ZIO[WalletAccessContext, ErrorResponse, Unit] = {
    for {
      maybeDIDState <- managedDIDService
        .getManagedDIDState(prismDID.asCanonical)
        .orDieWith(e => RuntimeException(s"Error occurred while getting DID from wallet: ${e.toString}"))
      initialResolveDID <- didService
        .resolveDID(prismDID)
        .orDieWith(e => RuntimeException(s"Error occurred while resolving the Prism DID: ${e.toString}"))
      oInitialDidData = initialResolveDID.map(_._2)
      mayBeResolveWithLongFormOrShortForm <-
        if (oInitialDidData.isEmpty) {
          for {
            oLongFormDid <- getLongFormPrismDID(prismDID, allowUnpublished)
              .orDieWith(e => RuntimeException(s"Error occurred while getting the long form Prism DID: ${e.toString}"))
              .provideSomeLayer(ZLayer.succeed(managedDIDService))
            longFormDid <- ZIO
              .fromOption(oLongFormDid)
              .orElse(ZIO.dieMessage(s"Longform of Prism DID cannot be found: $oLongFormDid"))
            resolvedDID <- didService
              .resolveDID(longFormDid)
              .orDieWith(e => RuntimeException(s"Error occurred while resolving the Prism DID: ${e.toString}"))
          } yield resolvedDID
        } else {
          ZIO.succeed(initialResolveDID)
        }
      maybeDidData = mayBeResolveWithLongFormOrShortForm.map(_._2)
      maybeMetadata = mayBeResolveWithLongFormOrShortForm.map(_._1)
      _ <- ZIO.when(role == Role.Holder) {
        ZIO
          .fromOption(maybeDidData.flatMap(_.publicKeys.find(_.purpose == VerificationRelationship.Authentication)))
          .orElseFail(ErrorResponse.badRequest(detail = Some(s"Authentication key not found for the $prismDID")))
      }
      _ <- ZIO.when(role == Role.Issuer)(
        ZIO
          .fromOption(maybeDidData.flatMap(_.publicKeys.find(_.purpose == VerificationRelationship.AssertionMethod)))
          .orElseFail(ErrorResponse.badRequest(detail = Some(s"AssertionMethod key not found for the $prismDID")))
      )
      _ <- (maybeDIDState.map(_.publicationState), maybeMetadata.map(_.deactivated)) match {
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
    } yield ()
  }
}

object IssueControllerImpl {
  val layer
      : URLayer[CredentialService & ConnectionService & DIDService & ManagedDIDService & AppConfig, IssueController] =
    ZLayer.fromFunction(IssueControllerImpl(_, _, _, _, _))
}
