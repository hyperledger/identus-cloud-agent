package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.oidc4vc.CredentialIssuer
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

import java.util.UUID

trait OIDC4VCIssuerMetadataRepository {
  def create(issuer: CredentialIssuer): URIO[WalletAccessContext, Unit]
  def findAllIssuerForWallet: URIO[WalletAccessContext, Seq[CredentialIssuer]]
  def findIssuer(issuerId: UUID): UIO[Option[CredentialIssuer]]
}

class InMemoryOIDC4VCIssuerMetadataRepository(store: Ref[Map[UUID, CredentialIssuer]])
    extends OIDC4VCIssuerMetadataRepository {

  override def create(issuer: CredentialIssuer): URIO[WalletAccessContext, Unit] =
    store.modify(m => () -> m.updated(issuer.id, issuer))

  override def findAllIssuerForWallet: URIO[WalletAccessContext, Seq[CredentialIssuer]] =
    store.get.map(_.values.toSeq)

  override def findIssuer(issuerId: UUID): UIO[Option[CredentialIssuer]] =
    store.get.map(_.get(issuerId))

}

object InMemoryOIDC4VCIssuerMetadataRepository {
  def layer: ULayer[OIDC4VCIssuerMetadataRepository] =
    ZLayer.fromZIO(
      for {
        store <- Ref.make(Map.empty)
      } yield InMemoryOIDC4VCIssuerMetadataRepository(store)
    )
}
