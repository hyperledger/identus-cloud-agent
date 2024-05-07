package org.hyperledger.identus.pollux.core.repository

import org.hyperledger.identus.pollux.core.model.oidc4vc.CredentialConfiguration
import org.hyperledger.identus.pollux.core.model.oidc4vc.CredentialIssuer
import org.hyperledger.identus.shared.models.WalletAccessContext
import org.hyperledger.identus.shared.models.WalletId
import zio.*

import java.net.URL
import java.time.Instant
import java.util.UUID

trait OIDC4VCIssuerMetadataRepository {
  def findIssuerById(issuerId: UUID): UIO[Option[CredentialIssuer]]
  def createIssuer(issuer: CredentialIssuer): URIO[WalletAccessContext, Unit]
  def findWalletIssuers: URIO[WalletAccessContext, Seq[CredentialIssuer]]
  def updateIssuer(issuerId: UUID, authorizationServer: Option[URL] = None): URIO[WalletAccessContext, Unit]
  def deleteIssuer(issuerId: UUID): URIO[WalletAccessContext, Unit]
  def createCredentialConfiguration(issuerId: UUID, config: CredentialConfiguration): URIO[WalletAccessContext, Unit]
  def findCredentialConfigurationsByIssuer(issuerId: UUID): UIO[Seq[CredentialConfiguration]]
  def findCredentialConfigurationById(
      issuerId: UUID,
      configurationId: String
  ): URIO[WalletAccessContext, Option[CredentialConfiguration]]
  def deleteCredentialConfiguration(issuerId: UUID, configurationId: String): URIO[WalletAccessContext, Unit]
}

class InMemoryOIDC4VCIssuerMetadataRepository(
    issuerStore: Ref[Map[WalletId, Seq[CredentialIssuer]]],
    credentialConfigStore: Ref[Map[(WalletId, UUID), Seq[CredentialConfiguration]]]
) extends OIDC4VCIssuerMetadataRepository {

  override def findIssuerById(issuerId: UUID): UIO[Option[CredentialIssuer]] =
    issuerStore.get.map(m => m.values.flatten.find(_.id == issuerId))

  override def createIssuer(issuer: CredentialIssuer): URIO[WalletAccessContext, Unit] =
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      _ <- issuerStore.modify(m => () -> m.updated(walletId, m.getOrElse(walletId, Nil) :+ issuer))
    } yield ()

  override def findWalletIssuers: URIO[WalletAccessContext, Seq[CredentialIssuer]] =
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      store <- issuerStore.get
    } yield store.getOrElse(walletId, Nil)

  override def updateIssuer(
      issuerId: UUID,
      authorizationServer: Option[URL]
  ): URIO[WalletAccessContext, Unit] =
    for {
      issuer <- findIssuerById(issuerId)
        .someOrElseZIO(ZIO.dieMessage("Update credential issuer fail. The issuer does not exist"))
      updatedAuthServerIssuer = authorizationServer
        .fold(issuer)(url => issuer.copy(authorizationServer = url))
      _ <- deleteIssuer(issuerId)
      _ <- createIssuer(updatedAuthServerIssuer.copy(updatedAt = Instant.now))
    } yield ()

  override def deleteIssuer(issuerId: UUID): URIO[WalletAccessContext, Unit] =
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      _ <- issuerStore.update(m => m.updated(walletId, m.getOrElse(walletId, Nil).filter(_.id != issuerId)))
      _ <- credentialConfigStore.update(m =>
        m.filterNot { case ((wid, issId), _) => wid == walletId && issId == issuerId }
      )
    } yield ()

  override def createCredentialConfiguration(
      issuerId: UUID,
      config: CredentialConfiguration
  ): URIO[WalletAccessContext, Unit] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      issuerExists <- issuerStore.get.map(_.getOrElse(walletId, Nil).exists(_.id == issuerId))
      configExists <- credentialConfigStore.get
        .map(m => m.getOrElse((walletId, issuerId), Nil).exists(_.configurationId == config.configurationId))
      _ <- ZIO
        .cond(issuerExists, (), s"Issuer with id $issuerId does not exist")
        .orDieWith(Exception(_))
      _ <- ZIO
        .cond(!configExists, (), s"Configuration with id ${config.configurationId} already exists")
        .orDieWith(Exception(_))
      _ <- credentialConfigStore
        .update(m => m.updated((walletId, issuerId), m.getOrElse((walletId, issuerId), Nil) :+ config))
    } yield ()
  }

  override def findCredentialConfigurationsByIssuer(
      issuerId: UUID
  ): UIO[Seq[CredentialConfiguration]] =
    credentialConfigStore.get.map { m =>
      m.collect { case ((_, iss), configs) if iss == issuerId => configs }.flatten.toSeq
    }

  // TODO: Implement
  override def findCredentialConfigurationById(
      issuerId: UUID,
      configurationId: String
  ): URIO[WalletAccessContext, Option[CredentialConfiguration]] = ???

  override def deleteCredentialConfiguration(
      issuerId: UUID,
      configurationId: String
  ): URIO[WalletAccessContext, Unit] =
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      _ <- credentialConfigStore.update(m =>
        m.updated(
          (walletId, issuerId),
          m.getOrElse((walletId, issuerId), Nil).filter(_.configurationId != configurationId)
        )
      )
    } yield ()

}

object InMemoryOIDC4VCIssuerMetadataRepository {
  def layer: ULayer[OIDC4VCIssuerMetadataRepository] =
    ZLayer.fromZIO(
      for {
        issuerStore <- Ref.make(Map.empty[WalletId, Seq[CredentialIssuer]])
        credentialConfigStore <- Ref.make(Map.empty[(WalletId, UUID), Seq[CredentialConfiguration]])
      } yield InMemoryOIDC4VCIssuerMetadataRepository(issuerStore, credentialConfigStore)
    )
}
