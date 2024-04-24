package org.hyperledger.identus.iam.entity.http.controller

import org.hyperledger.identus.agent.walletapi.model.error.EntityServiceError
import org.hyperledger.identus.api.http.model.PaginationInput
import org.hyperledger.identus.api.http.{ErrorResponse, RequestContext}
import org.hyperledger.identus.iam.entity.http.model.{CreateEntityRequest, EntityResponse, EntityResponsePage}
import zio.*

import java.util.UUID

trait EntityController {

  def createEntity(in: CreateEntityRequest)(implicit rc: RequestContext): IO[ErrorResponse, EntityResponse]
  def getEntity(id: UUID)(implicit rc: RequestContext): IO[ErrorResponse, EntityResponse]
  def getEntities(paginationIn: PaginationInput)(implicit rc: RequestContext): IO[ErrorResponse, EntityResponsePage]
  def updateEntityName(id: UUID, name: String)(implicit rc: RequestContext): IO[ErrorResponse, EntityResponse]
  def updateEntityWalletId(id: UUID, walletId: UUID)(implicit rc: RequestContext): IO[ErrorResponse, EntityResponse]
  def deleteEntity(id: UUID)(implicit rc: RequestContext): IO[ErrorResponse, Unit]
  def addApiKeyAuth(id: UUID, apiKey: String)(implicit rc: RequestContext): IO[ErrorResponse, Unit]
  def deleteApiKeyAuth(id: UUID, apiKey: String)(implicit rc: RequestContext): IO[ErrorResponse, Unit]
}

object EntityController {
  def domainToHttpError(error: EntityServiceError): ErrorResponse = {
    error match {
      case EntityServiceError.EntityStorageError(message: String) =>
        ErrorResponse.internalServerError("RepositoryError", detail = Option(message))
      case EntityServiceError.EntityNotFound(id, message) =>
        ErrorResponse.notFound(detail = Option(message))
      case EntityServiceError.EntityAlreadyExists(id, message) =>
        ErrorResponse.badRequest(detail = Option(message))
      case ewnf: EntityServiceError.EntityWalletNotFound =>
        ErrorResponse.badRequest(detail = Option(ewnf.message))
    }
  }
}
