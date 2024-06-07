package org.hyperledger.identus.pollux.credentialdefinition.controller

import org.hyperledger.identus.api.http.*
import org.hyperledger.identus.api.http.model.{Order, Pagination}
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
