package io.iohk.atala.pollux.core.service

import io.iohk.atala.pollux.core.model.oidc4vc.CredentialIssuer
import io.iohk.atala.pollux.core.repository.OIDC4VCIssuerMetadataRepository
import io.iohk.atala.pollux.core.service.OIDC4VCIssuerMetadataServiceError.IssuerIdNotFound
import io.iohk.atala.shared.models.Failure
import io.iohk.atala.shared.models.StatusCode
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.net.URL
import java.util.UUID
import java.{util => ju}

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
}

trait OIDC4VCIssuerMetadataService {
  def createCredentialIssuer(authorizationServer: URL): URIO[WalletAccessContext, CredentialIssuer]
  def getCredentialIssuer(issuerId: UUID): IO[IssuerIdNotFound, CredentialIssuer]
}

class OIDC4VCIssuerMetadataServiceImpl(repository: OIDC4VCIssuerMetadataRepository)
    extends OIDC4VCIssuerMetadataService {

  override def createCredentialIssuer(authorizationServer: URL): URIO[WalletAccessContext, CredentialIssuer] = {
    val issuer = CredentialIssuer(authorizationServer)
    repository.create(issuer).as(issuer)
  }

  override def getCredentialIssuer(issuerId: ju.UUID): IO[IssuerIdNotFound, CredentialIssuer] =
    repository
      .findIssuer(issuerId)
      .someOrFail(IssuerIdNotFound(issuerId))

}

object OIDC4VCIssuerMetadataServiceImpl {
  def layer: URLayer[OIDC4VCIssuerMetadataRepository, OIDC4VCIssuerMetadataService] = {
    ZLayer.fromFunction(OIDC4VCIssuerMetadataServiceImpl(_))
  }
}
