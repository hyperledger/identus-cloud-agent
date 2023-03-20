package io.iohk.atala.pollux.credentialschema.controller

import io.iohk.atala.agent.server.http.model.HttpServiceError.DomainError
import io.iohk.atala.api.http.model.{CollectionStats, Order, Pagination, PaginationInput}
import io.iohk.atala.api.http.*
import io.iohk.atala.pollux.core.service.CredentialSchemaService
import io.iohk.atala.pollux.core.service.CredentialSchemaService.Error.*
import io.iohk.atala.pollux.credentialschema.http.{
  CredentialSchemaInput,
  CredentialSchemaResponsePage,
  CredentialSchemaResponse,
  FilterInput
}
import zio.{IO, Task, ZIO, ZLayer}

import java.util.UUID

trait CredentialSchemaController {
  def createSchema(in: CredentialSchemaInput)(implicit
      rc: RequestContext
  ): IO[FailureResponse, CredentialSchemaResponse]

  def updateSchema(author: String, id: UUID, in: CredentialSchemaInput)(implicit
      rc: RequestContext
  ): IO[FailureResponse, CredentialSchemaResponse]

  def getSchemaByGuid(id: UUID)(implicit
      rc: RequestContext
  ): IO[FailureResponse, CredentialSchemaResponse]

  def delete(guid: UUID)(implicit
      rc: RequestContext
  ): IO[FailureResponse, CredentialSchemaResponse]

  def lookupSchemas(
      filter: FilterInput,
      pagination: Pagination,
      order: Option[Order]
  )(implicit
      rc: RequestContext
  ): IO[FailureResponse, CredentialSchemaResponsePage]
}

object CredentialSchemaController {
  def domainToHttpError(
      error: CredentialSchemaService.Error
  ): FailureResponse = {
    error match {
      case RepositoryError(cause: Throwable) =>
        InternalServerError(cause.getMessage)
      case NotFoundError(_, _, message) =>
        NotFound(message)
      case UpdateError(id, version, author, message) =>
        BadRequest(
          msg = s"Credential schema update error: id=$id, version=$version",
          errors = List(message)
        )
      case UnexpectedError(msg: String) => InternalServerError(msg)
    }
  }

  implicit def domainToHttpErrorIO[R, T](
      domainIO: ZIO[R, CredentialSchemaService.Error, T]
  ): ZIO[R, FailureResponse, T] = {
    domainIO.mapError(domainToHttpError)
  }
}
