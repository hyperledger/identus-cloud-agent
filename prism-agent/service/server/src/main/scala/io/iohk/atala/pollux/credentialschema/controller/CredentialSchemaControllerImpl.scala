package io.iohk.atala.pollux.credentialschema.controller

import io.iohk.atala.api.http.*
import io.iohk.atala.api.http.model.{CollectionStats, Order, Pagination}
import io.iohk.atala.pollux.core.model.CredentialSchema.FilteredEntries
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
import zio.{IO, Task, URLayer, ZIO, ZLayer}

import java.util.UUID

class CredentialSchemaControllerImpl(service: CredentialSchemaService) extends CredentialSchemaController {
  override def createSchema(
      in: CredentialSchemaInput
  )(implicit
      rc: RequestContext
  ): IO[FailureResponse, CredentialSchemaResponse] = {
    service
      .create(toDomain(in))
      .map(cs => fromDomain(cs).withBaseUri(rc.request.uri))
  }

  override def updateSchema(author: String, id: UUID, in: CredentialSchemaInput)(implicit
      rc: RequestContext
  ): IO[FailureResponse, CredentialSchemaResponse] = {
    service
      .update(id, toDomain(in).copy(author = author))
      .map(cs => fromDomain(cs).withBaseUri(rc.request.uri))
  }

  override def getSchemaByGuid(guid: UUID)(implicit
      rc: RequestContext
  ): IO[FailureResponse, CredentialSchemaResponse] = {
    service
      .getByGUID(guid)
      .map(
        fromDomain(_)
          .withSelf(rc.request.uri.toString)
      )
  }

  override def delete(guid: UUID)(implicit
      rc: RequestContext
  ): IO[FailureResponse, CredentialSchemaResponse] = {
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
  ): IO[FailureResponse, CredentialSchemaResponsePage] = {
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
}

object CredentialSchemaControllerImpl {
  val layer: URLayer[CredentialSchemaService, CredentialSchemaController] =
    ZLayer.fromFunction(CredentialSchemaControllerImpl(_))
}
