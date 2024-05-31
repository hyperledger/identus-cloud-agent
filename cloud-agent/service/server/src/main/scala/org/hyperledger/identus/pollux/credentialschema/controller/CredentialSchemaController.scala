package org.hyperledger.identus.pollux.credentialschema.controller

import org.hyperledger.identus.api.http.*
import org.hyperledger.identus.api.http.model.{Order, Pagination}
import org.hyperledger.identus.pollux.core.service.CredentialSchemaService
import org.hyperledger.identus.pollux.core.service.CredentialSchemaService.Error.*
import org.hyperledger.identus.pollux.credentialschema.http.{
  CredentialSchemaInput,
  CredentialSchemaResponse,
  CredentialSchemaResponsePage,
  FilterInput
}
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*
import zio.json.ast.Json

import java.util.UUID
import scala.language.implicitConversions

trait CredentialSchemaController {
  def createSchema(in: CredentialSchemaInput)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialSchemaResponse]

  def updateSchema(author: String, id: UUID, in: CredentialSchemaInput)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialSchemaResponse]

  def getSchemaByGuid(id: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, CredentialSchemaResponse]

  def getSchemaJsonByGuid(id: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, Json]

  def delete(guid: UUID)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialSchemaResponse]

  def lookupSchemas(
      filter: FilterInput,
      pagination: Pagination,
      order: Option[Order]
  )(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialSchemaResponsePage]
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
          detail = Option(s"Credential schema update error: id=$id, version=$version, author=$author, msg=$message")
        )
      case UnexpectedError(msg: String) =>
        ErrorResponse.internalServerError(detail = Option(msg))
      case CredentialSchemaValidationError(cause) =>
        ErrorResponse.badRequest(detail = Some(cause.message))
    }
  }

  implicit def domainToHttpErrorIO[R, T](
      domainIO: ZIO[R, CredentialSchemaService.Error, T]
  ): ZIO[R, ErrorResponse, T] = {
    domainIO.mapError(domainToHttpError)
  }
}
