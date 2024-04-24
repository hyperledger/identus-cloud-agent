package org.hyperledger.identus.pollux.credentialdefinition.controller

import org.hyperledger.identus.agent.walletapi.model.{ManagedDIDState, PublicationState}
import org.hyperledger.identus.agent.walletapi.service.ManagedDIDService
import org.hyperledger.identus.api.http.*
import org.hyperledger.identus.api.http.model.{CollectionStats, Order, Pagination}
import org.hyperledger.identus.castor.core.model.did.{LongFormPrismDID, PrismDID}
import org.hyperledger.identus.pollux.core.model.schema.CredentialDefinition.FilteredEntries
import org.hyperledger.identus.pollux.core.service.CredentialDefinitionService
import org.hyperledger.identus.pollux.core.service.CredentialDefinitionService.Error.*
import org.hyperledger.identus.pollux.credentialdefinition
import org.hyperledger.identus.pollux.credentialdefinition.controller.CredentialDefinitionController.domainToHttpErrorIO
import org.hyperledger.identus.pollux.credentialdefinition.http.CredentialDefinitionInput.toDomain
import org.hyperledger.identus.pollux.credentialdefinition.http.CredentialDefinitionResponse.fromDomain
import org.hyperledger.identus.pollux.credentialdefinition.http.{
  CredentialDefinitionInput,
  CredentialDefinitionResponse,
  CredentialDefinitionResponsePage,
  FilterInput
}
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*
import zio.json.ast.Json

import java.util.UUID

class CredentialDefinitionControllerImpl(service: CredentialDefinitionService, managedDIDService: ManagedDIDService)
    extends CredentialDefinitionController {
  override def createCredentialDefinition(
      in: CredentialDefinitionInput
  )(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialDefinitionResponse] = {
    (for {
      _ <- validatePrismDID(in.author)
      result <- service
        .create(toDomain(in))
        .map(cs => fromDomain(cs).withBaseUri(rc.request.uri))
    } yield result).mapError {
      case e: ErrorResponse                     => e
      case e: CredentialDefinitionService.Error => CredentialDefinitionController.domainToHttpError(e)
    }
  }

  override def getCredentialDefinitionByGuid(
      guid: UUID
  )(implicit rc: RequestContext): IO[ErrorResponse, CredentialDefinitionResponse] = {
    service
      .getByGUID(guid)
      .map(
        fromDomain(_)
          .withSelf(rc.request.uri.toString)
      )
  }

  override def getCredentialDefinitionInnerDefinitionByGuid(id: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, Json] = {
    service
      .getByGUID(id)
      .map(fromDomain(_).definition)
  }

  override def delete(guid: UUID)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialDefinitionResponse] = {
    service
      .delete(guid)
      .map(
        fromDomain(_)
          .withBaseUri(rc.request.uri)
      )
  }

  override def lookupCredentialDefinitions(
      filter: FilterInput,
      pagination: Pagination,
      order: Option[Order]
  )(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialDefinitionResponsePage] = {
    for {
      filteredEntries: FilteredEntries <- service.lookup(
        filter.toDomain,
        pagination.offset,
        pagination.limit
      )
      entries = filteredEntries.entries
        .map(fromDomain(_).withBaseUri(rc.request.uri))
        .toList
      page = CredentialDefinitionResponsePage(entries)
      stats = CollectionStats(filteredEntries.totalCount, filteredEntries.count)
    } yield CredentialDefinitionControllerLogic(rc, pagination, page, stats).result
  }

  private[this] def validatePrismDID(author: String) =
    for {
      authorDID <- ZIO
        .fromEither(PrismDID.fromString(author))
        .mapError(_ => ErrorResponse.badRequest(detail = Some(s"Unable to parse as a Prism DID: ${author}")))
      longFormPrismDID <- getLongForm(authorDID, true)
    } yield longFormPrismDID

  private[this] def getLongForm(
      did: PrismDID,
      allowUnpublishedIssuingDID: Boolean = false
  ): ZIO[WalletAccessContext, ErrorResponse, LongFormPrismDID] = {
    for {
      didState <- managedDIDService
        .getManagedDIDState(did.asCanonical)
        .mapError(e =>
          ErrorResponse.internalServerError(detail =
            Some(s"Error occurred while getting did from wallet: ${e.toString}")
          )
        )
        .someOrFail(ErrorResponse.notFound(detail = Some(s"Issuer DID does not exist in the wallet: $did")))
        .flatMap {
          case s @ ManagedDIDState(_, _, _: PublicationState.Published) => ZIO.succeed(s)
          case s =>
            ZIO.cond(
              allowUnpublishedIssuingDID,
              s,
              ErrorResponse.badRequest(detail = Some(s"Issuer DID must be published: $did"))
            )
        }
      longFormPrismDID = PrismDID.buildLongFormFromOperation(didState.createOperation)
    } yield longFormPrismDID
  }
}

object CredentialDefinitionControllerImpl {
  val layer: URLayer[CredentialDefinitionService & ManagedDIDService, CredentialDefinitionController] =
    ZLayer.fromFunction(CredentialDefinitionControllerImpl(_, _))
}
