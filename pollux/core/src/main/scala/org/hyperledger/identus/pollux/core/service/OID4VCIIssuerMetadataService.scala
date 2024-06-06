package org.hyperledger.identus.pollux.core.service

import org.hyperledger.identus.pollux.core.model.CredentialFormat
import org.hyperledger.identus.pollux.core.model.error.CredentialSchemaError.CredentialSchemaParsingError
import org.hyperledger.identus.pollux.core.model.error.CredentialSchemaError.SchemaError
import org.hyperledger.identus.pollux.core.model.error.CredentialSchemaError.URISyntaxError
import org.hyperledger.identus.pollux.core.model.oid4vci.CredentialConfiguration
import org.hyperledger.identus.pollux.core.model.oid4vci.CredentialIssuer
import org.hyperledger.identus.pollux.core.model.schema.CredentialSchema
import org.hyperledger.identus.pollux.core.repository.OID4VCIIssuerMetadataRepository
import org.hyperledger.identus.pollux.core.service.OID4VCIIssuerMetadataServiceError.CredentialConfigurationNotFound
import org.hyperledger.identus.pollux.core.service.OID4VCIIssuerMetadataServiceError.InvalidSchemaId
import org.hyperledger.identus.pollux.core.service.OID4VCIIssuerMetadataServiceError.IssuerIdNotFound
import org.hyperledger.identus.pollux.core.service.OID4VCIIssuerMetadataServiceError.UnsupportedCredentialFormat
import org.hyperledger.identus.shared.db.Errors.UnexpectedAffectedRow
import org.hyperledger.identus.shared.models.Failure
import org.hyperledger.identus.shared.models.StatusCode
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

import java.net.URI
import java.net.URL
import java.util.UUID

sealed trait OID4VCIIssuerMetadataServiceError(
    val statusCode: StatusCode,
    val userFacingMessage: String
) extends Failure {
  override val namespace: String = "OID4VCIIssuerMetadataServiceError"
}

object OID4VCIIssuerMetadataServiceError {
  final case class IssuerIdNotFound(issuerId: UUID)
      extends OID4VCIIssuerMetadataServiceError(
        StatusCode.NotFound,
        s"There is no credential issuer matching the provided identifier: issuerId=$issuerId"
      )

  final case class CredentialConfigurationNotFound(issuerId: UUID, configurationId: String)
      extends OID4VCIIssuerMetadataServiceError(
        StatusCode.NotFound,
        s"There is no credential configuration matching the provided identifier: issuerId=$issuerId, configurationId=$configurationId"
      )

  final case class InvalidSchemaId(schemaId: String, msg: String)
      extends OID4VCIIssuerMetadataServiceError(
        StatusCode.BadRequest,
        s"The schemaId $schemaId is not a valid URI syntax: $msg"
      )

  final case class UnsupportedCredentialFormat(format: CredentialFormat)
      extends OID4VCIIssuerMetadataServiceError(
        StatusCode.BadRequest,
        s"Unsupported credential format in OID4VCI protocol: $format"
      )
}

trait OID4VCIIssuerMetadataService {
  def getCredentialIssuer(issuerId: UUID): IO[IssuerIdNotFound, CredentialIssuer]
  def createCredentialIssuer(issuer: CredentialIssuer): URIO[WalletAccessContext, CredentialIssuer]
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
  def getCredentialConfigurationById(
      issuerId: UUID,
      configurationId: String
  ): ZIO[WalletAccessContext, CredentialConfigurationNotFound, CredentialConfiguration]
  def deleteCredentialConfiguration(
      issuerId: UUID,
      configurationId: String,
  ): ZIO[WalletAccessContext, CredentialConfigurationNotFound, Unit]
}

class OID4VCIIssuerMetadataServiceImpl(repository: OID4VCIIssuerMetadataRepository, uriDereferencer: URIDereferencer)
    extends OID4VCIIssuerMetadataService {

  override def createCredentialIssuer(issuer: CredentialIssuer): URIO[WalletAccessContext, CredentialIssuer] =
    repository.createIssuer(issuer).as(issuer)

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

  override def getCredentialConfigurationById(
      issuerId: UUID,
      configurationId: String
  ): ZIO[WalletAccessContext, CredentialConfigurationNotFound, CredentialConfiguration] =
    repository
      .findCredentialConfigurationById(issuerId, configurationId)
      .someOrFail(CredentialConfigurationNotFound(issuerId, configurationId))

  override def deleteCredentialConfiguration(
      issuerId: UUID,
      configurationId: String
  ): ZIO[WalletAccessContext, CredentialConfigurationNotFound, Unit] =
    repository
      .deleteCredentialConfiguration(issuerId, configurationId)
      .catchSomeDefect { case _: UnexpectedAffectedRow =>
        ZIO.fail(CredentialConfigurationNotFound(issuerId, configurationId))
      }
}

object OID4VCIIssuerMetadataServiceImpl {
  def layer: URLayer[OID4VCIIssuerMetadataRepository & URIDereferencer, OID4VCIIssuerMetadataService] = {
    ZLayer.fromFunction(OID4VCIIssuerMetadataServiceImpl(_, _))
  }
}
