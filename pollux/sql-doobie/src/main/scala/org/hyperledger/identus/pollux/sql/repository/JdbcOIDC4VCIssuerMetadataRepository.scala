package org.hyperledger.identus.pollux.sql.repository

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.transactor.Transactor
import org.hyperledger.identus.pollux.core.model.oidc4vc.CredentialConfiguration
import org.hyperledger.identus.pollux.core.model.oidc4vc.CredentialIssuer
import org.hyperledger.identus.pollux.core.repository.OIDC4VCIssuerMetadataRepository
import org.hyperledger.identus.shared.db.ContextAwareTask
import org.hyperledger.identus.shared.db.Implicits.{*, given}
import org.hyperledger.identus.shared.models.WalletAccessContext
import org.hyperledger.identus.shared.models.WalletId
import zio.*

import java.net.URL
import java.util.UUID

// TODO: implement all members
class JdbcOIDC4VCIssuerMetadataRepository(xa: Transactor[ContextAwareTask]) extends OIDC4VCIssuerMetadataRepository {

  override def findAllCredentialConfigurations(issuerId: UUID): UIO[Seq[CredentialConfiguration]] = ???

  override def findWalletIssuers: URIO[WalletAccessContext, Seq[CredentialIssuer]] = ???

  override def createIssuer(issuer: CredentialIssuer): URIO[WalletAccessContext, Unit] = {
    val cxnIO = (walletId: WalletId) => sql"""
        |INSERT INTO public.issuer_metadata (
        |  id,
        |  authorization_server,
        |  created_at,
        |  updated_at,
        |  wallet_id
        |) VALUES (
        |  ${issuer.id},
        |  ${issuer.authorizationServer},
        |  ${issuer.createdAt},
        |  ${issuer.updatedAt},
        |  ${walletId}
        |)
        """.stripMargin.update

    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      _ <- cxnIO(walletId).run.transactWallet(xa).ensureOneAffectedRowOrDie
    } yield ()
  }

  override def deleteCredentialConfiguration(
      issuerId: UUID,
      configurationId: String
  ): URIO[WalletAccessContext, Unit] = ???

  override def findIssuer(issuerId: UUID): UIO[Option[CredentialIssuer]] = ???

  override def updateIssuer(
      issuerId: UUID,
      authorizationServer: Option[URL]
  ): URIO[WalletAccessContext, CredentialIssuer] = ???

  override def createCredentialConfiguration(
      issuerId: UUID,
      config: CredentialConfiguration
  ): URIO[WalletAccessContext, Unit] = ???

  override def deleteIssuer(issuerId: UUID): URIO[WalletAccessContext, Unit] = ???

}

object JdbcOIDC4VCIssuerMetadataRepository {
  val layer: URLayer[Transactor[ContextAwareTask], OIDC4VCIssuerMetadataRepository] =
    ZLayer.fromFunction(new JdbcOIDC4VCIssuerMetadataRepository(_))
}
