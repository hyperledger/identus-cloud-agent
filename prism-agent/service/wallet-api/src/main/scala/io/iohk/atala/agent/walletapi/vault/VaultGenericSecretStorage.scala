package io.iohk.atala.agent.walletapi.vault

import io.iohk.atala.agent.walletapi.storage.GenericSecret
import io.iohk.atala.agent.walletapi.storage.GenericSecretStorage
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*
import zio.json.ast.Json

class VaultGenericSecretStorage(vaultKV: VaultKVClient) extends GenericSecretStorage {

  override def set[K, V](key: K, secret: V)(implicit ev: GenericSecret[K, V]): RIO[WalletAccessContext, Unit] = {
    val payload = ev.encodeValue(secret)
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      path = constructKeyPath(walletId)(key)
      alreadyExist <- vaultKV.get[Json](path).map(_.isDefined)
      _ <- vaultKV
        .set[Json](path, payload)
        .when(!alreadyExist)
        .someOrFail(Exception(s"Secret on path $path already exists."))
    } yield ()
  }

  override def get[K, V](key: K)(implicit ev: GenericSecret[K, V]): RIO[WalletAccessContext, Option[V]] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      path = constructKeyPath(walletId)(key)
      json <- vaultKV.get[Json](path)
      result <- json.fold(ZIO.none)(json => ZIO.fromTry(ev.decodeValue(json)).asSome)
    } yield result
  }

  private def constructKeyPath[K, V](walletId: WalletId)(key: K)(implicit ev: GenericSecret[K, V]): String = {
    val keyPath = ev.keyPath(key)
    s"secret/${walletId.toUUID}/generic-secrets/$keyPath"
  }

}

object VaultGenericSecretStorage {
  def layer: URLayer[VaultKVClient, GenericSecretStorage] = ZLayer.fromFunction(VaultGenericSecretStorage(_))
}
