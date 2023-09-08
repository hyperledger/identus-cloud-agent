package io.iohk.atala.agent.walletapi.vault

import io.iohk.atala.agent.walletapi.storage.DIDSecret
import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*

class VaultDIDSecretStorage(vaultKV: VaultKVClient) extends DIDSecretStorage {

  override def insertKey(did: DidId, keyId: String, didSecret: DIDSecret): RIO[WalletAccessContext, Int] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      path = peerDidKeyPath(walletId)(did, keyId)
      alreadyExist <- vaultKV.get[DIDSecret](path).map(_.isDefined)
      _ <- vaultKV
        .set[DIDSecret](path, didSecret)
        .when(!alreadyExist)
        .someOrFail(Exception(s"Secret on path $path already exists."))
    } yield 1
  }

  override def getKey(did: DidId, keyId: String, schemaId: String): RIO[WalletAccessContext, Option[DIDSecret]] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      path = peerDidKeyPath(walletId)(did, keyId)
      keyPair <- vaultKV.get[DIDSecret](path)
    } yield keyPair
  }

  private def peerDidKeyPath(walletId: WalletId)(did: DidId, keyId: String): String = {
    s"secret/${walletId.toUUID}/dids/peer/${did.value}/keys/$keyId"
  }
}

object VaultDIDSecretStorage {
  def layer: URLayer[VaultKVClient, DIDSecretStorage] = ZLayer.fromFunction(VaultDIDSecretStorage(_))
}
