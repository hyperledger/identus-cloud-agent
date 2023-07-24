package io.iohk.atala.pollux.credentialschema.controller

import io.iohk.atala.agent.walletapi.model.{ManagedDIDState, PublicationState}
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.api.http.*
import io.iohk.atala.api.http.model.{CollectionStats, Order, Pagination}
import io.iohk.atala.castor.core.model.did.{LongFormPrismDID, PrismDID}
import io.iohk.atala.pollux.core.model.schema.CredentialSchema.FilteredEntries
import io.iohk.atala.pollux.core.service.CredentialSchemaService
import io.iohk.atala.pollux.core.service.CredentialSchemaService.Error.*
import io.iohk.atala.pollux.credentialschema.controller.CredentialSchemaController.domainToHttpErrorIO
import io.iohk.atala.pollux.credentialschema.http.CredentialSchemaInput.toDomain
import io.iohk.atala.pollux.credentialschema.http.CredentialSchemaResponse.fromDomain
import io.iohk.atala.pollux.credentialschema.http.{
  CredentialSchemaInput,
  CredentialSchemaResponse,
  CredentialSchemaResponsePage,
  FilterInput
}
import zio.*

import java.util.UUID
import io.iohk.atala.shared.models.WalletAccessContext

class CredentialSchemaControllerImpl(service: CredentialSchemaService, managedDIDService: ManagedDIDService)
    extends CredentialSchemaController {
  override def createSchema(
      in: CredentialSchemaInput
  )(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialSchemaResponse] = {
    (for {
      _ <- validatePrismDID(in.author)
      result <- service
        .create(toDomain(in))
        .map(cs => fromDomain(cs).withBaseUri(rc.request.uri))
    } yield result).mapError {
      case e: ErrorResponse                 => e
      case e: CredentialSchemaService.Error => CredentialSchemaController.domainToHttpError(e)
    }
  }

  override def updateSchema(author: String, id: UUID, in: CredentialSchemaInput)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialSchemaResponse] = {
    (for {
      _ <- validatePrismDID(in.author)
      result <- service
        .update(id, toDomain(in).copy(author = author))
        .map(cs => fromDomain(cs).withBaseUri(rc.request.uri))
    } yield result).mapError {
      case e: ErrorResponse                 => e
      case e: CredentialSchemaService.Error => CredentialSchemaController.domainToHttpError(e)
    }
  }

  override def getSchemaByGuid(guid: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, CredentialSchemaResponse] = {
    service
      .getByGUID(guid)
      .map(
        fromDomain(_)
          .withSelf(rc.request.uri.toString)
      )
  }

  override def delete(guid: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, CredentialSchemaResponse] = {
    service
      .delete(guid)
      .map(
        fromDomain(_)
          .withBaseUri(rc.request.uri)
      )
  }

  override def lookupSchemas(
      filter: FilterInput,
      pagination: Pagination,
      order: Option[Order]
  )(implicit
      rc: RequestContext
  ): IO[ErrorResponse, CredentialSchemaResponsePage] = {
    for {
      filteredEntries: FilteredEntries <- service.lookup(
        filter.toDomain,
        pagination.offset,
        pagination.limit
      )
      entries = filteredEntries.entries
        .map(fromDomain(_).withBaseUri(rc.request.uri))
        .toList
      page = CredentialSchemaResponsePage(entries)
      stats = CollectionStats(filteredEntries.totalCount, filteredEntries.count)
    } yield CredentialSchemaControllerLogic(rc, pagination, page, stats).result
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

object CredentialSchemaControllerImpl {
  val layer: URLayer[CredentialSchemaService & ManagedDIDService, CredentialSchemaController] =
    ZLayer.fromFunction(CredentialSchemaControllerImpl(_, _))
}
