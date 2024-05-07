package org.hyperledger.identus.pollux.core.service

import org.hyperledger.identus.pollux.core.model.CredentialFormat
import org.hyperledger.identus.pollux.core.model.error.CredentialSchemaError.CredentialSchemaParsingError
import org.hyperledger.identus.pollux.core.model.error.CredentialSchemaError.SchemaError
import org.hyperledger.identus.pollux.core.model.error.CredentialSchemaError.URISyntaxError
import org.hyperledger.identus.pollux.core.model.oidc4vc.CredentialConfiguration
import org.hyperledger.identus.pollux.core.model.oidc4vc.CredentialIssuer
import org.hyperledger.identus.pollux.core.model.schema.CredentialSchema
import org.hyperledger.identus.pollux.core.repository.OIDC4VCIssuerMetadataRepository
import org.hyperledger.identus.pollux.core.service.OIDC4VCIssuerMetadataServiceError.CredentialConfigurationNotFound
import org.hyperledger.identus.pollux.core.service.OIDC4VCIssuerMetadataServiceError.InvalidSchemaId
import org.hyperledger.identus.pollux.core.service.OIDC4VCIssuerMetadataServiceError.IssuerIdNotFound
import org.hyperledger.identus.pollux.core.service.OIDC4VCIssuerMetadataServiceError.UnsupportedCredentialFormat
import org.hyperledger.identus.shared.db.Errors.UnexpectedAffectedRow
import org.hyperledger.identus.shared.models.Failure
import org.hyperledger.identus.shared.models.StatusCode
import org.hyperledger.identus.shared.models.WalletAccessContext
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

  final case class CredentialConfigurationNotFound(issuerId: UUID, configurationId: String)
      extends OIDC4VCIssuerMetadataServiceError(
        StatusCode.NotFound,
        s"There is no credential configuration matching the provided identifier: issuerId=$issuerId, configurationId=$configurationId"
      )

  final case class InvalidSchemaId(schemaId: String, msg: String)
      extends OIDC4VCIssuerMetadataServiceError(
        StatusCode.BadRequest,
        s"The schemaId $schemaId is not a valid URI syntax: $msg"
      )

  final case class UnsupportedCredentialFormat(format: CredentialFormat)
      extends OIDC4VCIssuerMetadataServiceError(
        StatusCode.BadRequest,
        s"Unsupported credential format in OIDC4VC protocol: $format"
      )
}

trait OIDC4VCIssuerMetadataService {
  def getCredentialIssuer(issuerId: UUID): IO[IssuerIdNotFound, CredentialIssuer]
  def createCredentialIssuer(authorizationServer: URL): URIO[WalletAccessContext, CredentialIssuer]
  def getCredentialIssuers: URIO[WalletAccessContext, Seq[CredentialIssuer]]
  def updateCredentialIssuer(
      issuerId: UUID,
      authorizationServer: Option[URL] = None
  ): ZIO[WalletAccessContext, IssuerIdNotFound, CredentialIssuer]
  def deleteCredentialIssuer(issuerId: UUID): ZIO[WalletAccessContext, IssuerIdNotFound, Unit]
  def createCredentialConfiguration(
      issuerId: UUID,
      format: CredentialFormat,
      configurationId: String,
      schemaId: String
  ): ZIO[WalletAccessContext, InvalidSchemaId | UnsupportedCredentialFormat, CredentialConfiguration]
  def getCredentialConfigurations(
      issuerId: UUID
  ): IO[IssuerIdNotFound, Seq[CredentialConfiguration]]
  def deleteCredentialConfiguration(
      issuerId: UUID,
      configurationId: String,
  ): ZIO[WalletAccessContext, IssuerIdNotFound | CredentialConfigurationNotFound, Unit]
}

class OIDC4VCIssuerMetadataServiceImpl(repository: OIDC4VCIssuerMetadataRepository, uriDereferencer: URIDereferencer)
    extends OIDC4VCIssuerMetadataService {

  override def createCredentialIssuer(authorizationServer: URL): URIO[WalletAccessContext, CredentialIssuer] = {
    val issuer = CredentialIssuer(authorizationServer)
    repository.createIssuer(issuer).as(issuer)
  }

  override def getCredentialIssuers: URIO[WalletAccessContext, Seq[CredentialIssuer]] =
    repository.findWalletIssuers

  override def getCredentialIssuer(issuerId: UUID): IO[IssuerIdNotFound, CredentialIssuer] =
    repository
      .findIssuerById(issuerId)
      .someOrFail(IssuerIdNotFound(issuerId))

  override def updateCredentialIssuer(
      issuerId: UUID,
      authorizationServer: Option[URL]
  ): ZIO[WalletAccessContext, IssuerIdNotFound, CredentialIssuer] =
    for {
      _ <- repository
        .updateIssuer(issuerId, authorizationServer = authorizationServer)
        .catchSomeDefect { case _: UnexpectedAffectedRow =>
          ZIO.fail(IssuerIdNotFound(issuerId))
        }
      updatedIssuer <- getCredentialIssuer(issuerId)
    } yield updatedIssuer

  override def deleteCredentialIssuer(issuerId: UUID): ZIO[WalletAccessContext, IssuerIdNotFound, Unit] =
    repository
      .deleteIssuer(issuerId)
      .catchSomeDefect { case _: UnexpectedAffectedRow =>
        ZIO.fail(IssuerIdNotFound(issuerId))
      }

  override def createCredentialConfiguration(
      issuerId: UUID,
      format: CredentialFormat,
      configurationId: String,
      schemaId: String
  ): ZIO[WalletAccessContext, InvalidSchemaId | UnsupportedCredentialFormat, CredentialConfiguration] = {
    for {
      _ <- format match {
        case CredentialFormat.JWT => ZIO.unit
        case f                    => ZIO.fail(UnsupportedCredentialFormat(f))
      }
      schemaUri <- ZIO.attempt(new URI(schemaId)).mapError(t => InvalidSchemaId(schemaId, t.getMessage))
      _ <- CredentialSchema
        .validSchemaValidator(schemaUri.toString(), uriDereferencer)
        .catchAll {
          case e: URISyntaxError => ZIO.fail(InvalidSchemaId(schemaId, e.message))
          case _: CredentialSchemaParsingError | _: SchemaError =>
            ZIO.fail(InvalidSchemaId(schemaId, "The schema URI does not contain a valid schema response"))
          case e => ZIO.dieMessage(s"Unexpected error when resolving schema $schemaId: $e")
        }
      now <- ZIO.clockWith(_.instant)
      config = CredentialConfiguration(
        configurationId = configurationId,
        format = CredentialFormat.JWT,
        schemaId = schemaUri,
        createdAt = now
      )
      _ <- repository.createCredentialConfiguration(issuerId, config)
    } yield config
  }

  override def getCredentialConfigurations(
      issuerId: UUID
  ): IO[IssuerIdNotFound, Seq[CredentialConfiguration]] =
    repository
      .findIssuerById(issuerId)
      .someOrFail(IssuerIdNotFound(issuerId))
      .flatMap(_ => repository.findCredentialConfigurationsByIssuer(issuerId))

  override def deleteCredentialConfiguration(
      issuerId: UUID,
      configurationId: String
  ): ZIO[WalletAccessContext, IssuerIdNotFound | CredentialConfigurationNotFound, Unit] =
    repository
      .deleteCredentialConfiguration(issuerId, configurationId)
      .catchSomeDefect { case _: UnexpectedAffectedRow =>
        ZIO.fail(CredentialConfigurationNotFound(issuerId, configurationId))
      }
}

object OIDC4VCIssuerMetadataServiceImpl {
  def layer: URLayer[OIDC4VCIssuerMetadataRepository & URIDereferencer, OIDC4VCIssuerMetadataService] = {
    ZLayer.fromFunction(OIDC4VCIssuerMetadataServiceImpl(_, _))
  }
}
