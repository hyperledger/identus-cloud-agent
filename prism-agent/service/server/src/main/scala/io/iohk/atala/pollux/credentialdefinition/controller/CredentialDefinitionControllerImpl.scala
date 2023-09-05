package io.iohk.atala.pollux.credentialdefinition.controller

import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.agent.walletapi.model.PublicationState
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.api.http.*
import io.iohk.atala.api.http.model.CollectionStats
import io.iohk.atala.api.http.model.Order
import io.iohk.atala.api.http.model.Pagination
import io.iohk.atala.castor.core.model.did.LongFormPrismDID
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.pollux.core.model.schema.CredentialDefinition.FilteredEntries
import io.iohk.atala.pollux.core.service.CredentialDefinitionService
import io.iohk.atala.pollux.core.service.CredentialDefinitionService.Error.*
import io.iohk.atala.pollux.credentialdefinition
import io.iohk.atala.pollux.credentialdefinition.controller.CredentialDefinitionController.domainToHttpErrorIO
import io.iohk.atala.pollux.credentialdefinition.http.CredentialDefinitionInput
import io.iohk.atala.pollux.credentialdefinition.http.CredentialDefinitionInput.toDomain
import io.iohk.atala.pollux.credentialdefinition.http.CredentialDefinitionResponse
import io.iohk.atala.pollux.credentialdefinition.http.CredentialDefinitionResponse.fromDomain
import io.iohk.atala.pollux.credentialdefinition.http.CredentialDefinitionResponsePage
import io.iohk.atala.pollux.credentialdefinition.http.FilterInput
import zio.*

import java.util.UUID

class CredentialDefinitionControllerImpl(service: CredentialDefinitionService, managedDIDService: ManagedDIDService)
    extends CredentialDefinitionController {
  override def createCredentialDefinition(
      in: CredentialDefinitionInput
  )(implicit
      rc: RequestContext
  ): IO[ErrorResponse, CredentialDefinitionResponse] = {
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

  override def delete(guid: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, CredentialDefinitionResponse] = {
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
  ): IO[ErrorResponse, CredentialDefinitionResponsePage] = {
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
  ): IO[ErrorResponse, LongFormPrismDID] = {
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
