package org.hyperledger.identus.pollux.credentialschema.controller

import org.hyperledger.identus.api.http.*
import org.hyperledger.identus.api.http.model.{Order, Pagination}
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
