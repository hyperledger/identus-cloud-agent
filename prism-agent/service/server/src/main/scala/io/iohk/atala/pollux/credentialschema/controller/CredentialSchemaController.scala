package io.iohk.atala.pollux.credentialschema.controller

import io.iohk.atala.api.http.*
import io.iohk.atala.api.http.model.{CollectionStats, Order, Pagination, PaginationInput}
import io.iohk.atala.pollux.core.service.CredentialSchemaService
import io.iohk.atala.pollux.core.service.CredentialSchemaService.Error.*
import io.iohk.atala.pollux.credentialschema.http.{
  CredentialSchemaInput,
  CredentialSchemaResponse,
  CredentialSchemaResponsePage,
  FilterInput
}
import zio.{IO, Task, ZIO, ZLayer}
import scala.language.implicitConversions

import java.util.UUID

trait CredentialSchemaController {
  def createSchema(in: CredentialSchemaInput)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, CredentialSchemaResponse]

  def updateSchema(author: String, id: UUID, in: CredentialSchemaInput)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, CredentialSchemaResponse]

  def getSchemaByGuid(id: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, CredentialSchemaResponse]

  def delete(guid: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, CredentialSchemaResponse]

  def lookupSchemas(
      filter: FilterInput,
      pagination: Pagination,
      order: Option[Order]
  )(implicit
      rc: RequestContext
  ): IO[ErrorResponse, CredentialSchemaResponsePage]
}

object CredentialSchemaController {
  def domainToHttpError(
      error: CredentialSchemaService.Error
  ): ErrorResponse = {
    error match {
      case RepositoryError(cause: Throwable) =>
        ErrorResponse.internalServerError("RepositoryError", detail = Option(cause.toString))
      case NotFoundError(_, _, message) =>
        ErrorResponse.notFound(detail = Option(message))
      case UpdateError(id, version, author, message) =>
        ErrorResponse.badRequest(
          title = "CredentialSchemaUpdateError",
          detail = Option(s"Credential schema update error: id=$id, version=$version, author=$author"),
          instance = message
        )
      case UnexpectedError(msg: String) =>
        ErrorResponse.internalServerError(detail = Option(msg))
      case CredentialSchemaValidationError(cause) =>
        ErrorResponse.badRequest(detail = Some(cause.userMessage))
    }
  }

  implicit def domainToHttpErrorIO[R, T](
      domainIO: ZIO[R, CredentialSchemaService.Error, T]
  ): ZIO[R, ErrorResponse, T] = {
    domainIO.mapError(domainToHttpError)
  }
}
