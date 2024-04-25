package org.hyperledger.identus.pollux.credentialdefinition.controller

import org.hyperledger.identus.api.http.*
import org.hyperledger.identus.api.http.model.{Order, Pagination}
import org.hyperledger.identus.pollux.core.service.CredentialDefinitionService
import org.hyperledger.identus.pollux.core.service.CredentialDefinitionService.Error.*
import org.hyperledger.identus.pollux.credentialdefinition.http.{
  CredentialDefinitionInput,
  CredentialDefinitionResponse,
  CredentialDefinitionResponsePage,
  FilterInput
}
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

import java.util.UUID
import scala.language.implicitConversions

trait CredentialDefinitionController {
  def createCredentialDefinition(in: CredentialDefinitionInput)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialDefinitionResponse]

  def getCredentialDefinitionByGuid(id: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, CredentialDefinitionResponse]

  def getCredentialDefinitionInnerDefinitionByGuid(id: UUID)(implicit
      rc: RequestContext
  ): IO[ErrorResponse, zio.json.ast.Json]

  def delete(guid: UUID)(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialDefinitionResponse]

  def lookupCredentialDefinitions(
      filter: FilterInput,
      pagination: Pagination,
      order: Option[Order]
  )(implicit
      rc: RequestContext
  ): ZIO[WalletAccessContext, ErrorResponse, CredentialDefinitionResponsePage]

}

object CredentialDefinitionController {
  def domainToHttpError(
      error: CredentialDefinitionService.Error
  ): ErrorResponse = {
    error match {
      case RepositoryError(cause: Throwable) =>
        ErrorResponse.internalServerError("RepositoryError", detail = Option(cause.toString))
      case NotFoundError(_, _, message) =>
        ErrorResponse.notFound(detail = Option(message))
      case UpdateError(id, version, author, message) =>
        ErrorResponse.badRequest(
          title = "CredentialDefinitionUpdateError",
          detail = Option(s"Credential definition update error: id=$id, version=$version, author=$author, msg=$message")
        )
      case CredentialDefinitionCreationError(msg: String) =>
        ErrorResponse.badRequest(detail = Option(msg))
      case UnexpectedError(msg: String) =>
        ErrorResponse.internalServerError(detail = Option(msg))
      case CredentialDefinitionValidationError(cause) =>
        ErrorResponse.badRequest(detail = Some(cause.message))
    }
  }

  implicit def domainToHttpErrorIO[R, T](
      domainIO: ZIO[R, CredentialDefinitionService.Error, T]
  ): ZIO[R, ErrorResponse, T] = {
    domainIO.mapError(domainToHttpError)
  }
}
