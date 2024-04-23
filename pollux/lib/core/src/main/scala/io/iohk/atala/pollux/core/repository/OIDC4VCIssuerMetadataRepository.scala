package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.oidc4vc.CredentialConfiguration
import io.iohk.atala.pollux.core.model.oidc4vc.CredentialIssuer
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

trait OIDC4VCIssuerMetadataRepository {
  def createIssuer(issuer: CredentialIssuer): URIO[WalletAccessContext, Unit]
  def findAllIssuerForWallet: URIO[WalletAccessContext, Seq[CredentialIssuer]]
  def findIssuer(issuerId: UUID): UIO[Option[CredentialIssuer]]

  def createCredentialConfiguration(issuerId: UUID, config: CredentialConfiguration): URIO[WalletAccessContext, Unit]
  def findAllCredentialConfigurations(issuerId: UUID): UIO[Seq[CredentialConfiguration]]
}

class InMemoryOIDC4VCIssuerMetadataRepository(
    issuerStore: Ref[Map[UUID, CredentialIssuer]],
    credentialConfigStore: Ref[Map[(UUID, String), CredentialConfiguration]]
) extends OIDC4VCIssuerMetadataRepository {

  override def createIssuer(issuer: CredentialIssuer): URIO[WalletAccessContext, Unit] =
    issuerStore.modify(m => () -> m.updated(issuer.id, issuer))

  override def findAllIssuerForWallet: URIO[WalletAccessContext, Seq[CredentialIssuer]] =
    issuerStore.get.map(_.values.toSeq)

  override def findIssuer(issuerId: UUID): UIO[Option[CredentialIssuer]] =
    issuerStore.get.map(_.get(issuerId))

  override def createCredentialConfiguration(
      issuerId: UUID,
      config: CredentialConfiguration
  ): URIO[WalletAccessContext, Unit] = {
    for {
      issuerExists <- issuerStore.get.map(_.contains(issuerId))
      configExists <- credentialConfigStore.get.map(_.contains((issuerId, config.configurationId)))
      _ <- ZIO
        .cond(issuerExists, (), s"Issuer with id $issuerId does not exist")
        .orDieWith(Exception(_))
      _ <- ZIO
        .cond(!configExists, (), s"Configuration with id ${config.configurationId} already exists")
        .orDieWith(Exception(_))
      _ <- credentialConfigStore.update(_.updated((issuerId, config.configurationId), config))
    } yield ()
  }

  override def findAllCredentialConfigurations(
      issuerId: UUID
  ): UIO[Seq[CredentialConfiguration]] =
    credentialConfigStore.get.map(_.filter(_._1._1 == issuerId).values.toSeq)

}

object InMemoryOIDC4VCIssuerMetadataRepository {
  def layer: ULayer[OIDC4VCIssuerMetadataRepository] =
    ZLayer.fromZIO(
      for {
        issuerStore <- Ref.make(Map.empty[UUID, CredentialIssuer])
        credentialConfigStore <- Ref.make(Map.empty[(UUID, String), CredentialConfiguration])
      } yield InMemoryOIDC4VCIssuerMetadataRepository(issuerStore, credentialConfigStore)
    )
}
