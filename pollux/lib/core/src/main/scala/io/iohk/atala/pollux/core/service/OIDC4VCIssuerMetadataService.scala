package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.model.CredentialFormat
import io.iohk.atala.pollux.core.model.error.CredentialSchemaError.URISyntaxError
import io.iohk.atala.pollux.core.model.oidc4vc.CredentialConfiguration
import io.iohk.atala.pollux.core.model.oidc4vc.CredentialIssuer
import io.iohk.atala.pollux.core.model.schema.CredentialSchema
import io.iohk.atala.pollux.core.repository.OIDC4VCIssuerMetadataRepository
import io.iohk.atala.pollux.core.service.OIDC4VCIssuerMetadataServiceError.InvalidSchemaId
import io.iohk.atala.pollux.core.service.OIDC4VCIssuerMetadataServiceError.IssuerIdNotFound
import io.iohk.atala.shared.models.Failure
import io.iohk.atala.shared.models.StatusCode
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.net.URI
import java.net.URL
import java.util.UUID

sealed trait OIDC4VCIssuerMetadataServiceError(
    val statusCode: StatusCode,
    val userFacingMessage: String
) extends Failure

object OIDC4VCIssuerMetadataServiceError {
  final case class IssuerIdNotFound(issuerId: UUID)
      extends OIDC4VCIssuerMetadataServiceError(
        StatusCode.NotFound,
        s"There is no credential issuer matching the provided identifier: issuerId=$issuerId"
      )

  final case class InvalidSchemaId(schemaId: String, msg: String)
      extends OIDC4VCIssuerMetadataServiceError(
        StatusCode.BadRequest,
        s"The schemaId $schemaId is not a valid URI syntax: $msg"
      )
}

trait OIDC4VCIssuerMetadataService {
  def createCredentialIssuer(authorizationServer: URL): URIO[WalletAccessContext, CredentialIssuer]
  def getCredentialIssuer(issuerId: UUID): IO[IssuerIdNotFound, CredentialIssuer]

  def createCredentialConfiguration(
      issuerId: UUID,
      configurationId: String,
      schemaId: String
  ): ZIO[WalletAccessContext, InvalidSchemaId, CredentialConfiguration]
  def listCredentialConfiguration(
      issuerId: UUID
  ): IO[IssuerIdNotFound, Seq[CredentialConfiguration]]
}

class OIDC4VCIssuerMetadataServiceImpl(repository: OIDC4VCIssuerMetadataRepository, uriDereferencer: URIDereferencer)
    extends OIDC4VCIssuerMetadataService {

  override def createCredentialIssuer(authorizationServer: URL): URIO[WalletAccessContext, CredentialIssuer] = {
    val issuer = CredentialIssuer(authorizationServer)
    repository.createIssuer(issuer).as(issuer)
  }

  override def getCredentialIssuer(issuerId: UUID): IO[IssuerIdNotFound, CredentialIssuer] =
    repository
      .findIssuer(issuerId)
      .someOrFail(IssuerIdNotFound(issuerId))

  override def createCredentialConfiguration(
      issuerId: UUID,
      configurationId: String,
      schemaId: String
  ): ZIO[WalletAccessContext, InvalidSchemaId, CredentialConfiguration] = {
    for {
      schemaUri <- ZIO.attempt(new URI(schemaId)).mapError(t => InvalidSchemaId(schemaId, t.getMessage))
      jsonSchema <- CredentialSchema
        .resolveJWTSchema(schemaUri, uriDereferencer)
        .catchAll {
          case e: URISyntaxError => ZIO.fail(InvalidSchemaId(schemaId, e.message))
          case e                 => ZIO.dieMessage(s"Unexpected error when resolving schema $schemaId: $e")
        }
      config = CredentialConfiguration(
        configurationId = configurationId,
        format = CredentialFormat.JWT,
        schemaId = schemaUri,
        dereferencedSchema = jsonSchema
      )
      _ <- repository.createCredentialConfiguration(issuerId, config)
    } yield config
  }

  override def listCredentialConfiguration(
      issuerId: UUID
  ): IO[IssuerIdNotFound, Seq[CredentialConfiguration]] =
    repository
      .findIssuer(issuerId)
      .someOrFail(IssuerIdNotFound(issuerId))
      .flatMap(_ => repository.findAllCredentialConfigurations(issuerId))

}

object OIDC4VCIssuerMetadataServiceImpl {
  def layer: URLayer[OIDC4VCIssuerMetadataRepository & URIDereferencer, OIDC4VCIssuerMetadataService] = {
    ZLayer.fromFunction(OIDC4VCIssuerMetadataServiceImpl(_, _))
  }
}
