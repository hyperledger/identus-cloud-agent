package io.iohk.atala.agent.walletapi.vault

import io.iohk.atala.agent.walletapi.storage.GenericSecret
import io.iohk.atala.agent.walletapi.storage.GenericSecretStorage
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.*
import zio.json.ast.Json

import java.nio.charset.StandardCharsets

class VaultGenericSecretStorage(vaultKV: VaultKVClient, useSemanticPath: Boolean) extends GenericSecretStorage {

  override def set[K, V](key: K, secret: V)(implicit ev: GenericSecret[K, V]): RIO[WalletAccessContext, Unit] = {
    val payload = ev.encodeValue(secret)
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      (path, metadata) = constructKeyPath(walletId)(key)
      alreadyExist <- vaultKV.get[Json](path).map(_.isDefined)
      _ <- vaultKV
        .set[Json](path, payload, metadata)
        .when(!alreadyExist)
        .someOrFail(Exception(s"Secret on path $path already exists."))
    } yield ()
  }

  override def get[K, V](key: K)(implicit ev: GenericSecret[K, V]): RIO[WalletAccessContext, Option[V]] = {
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      (path, _) = constructKeyPath(walletId)(key)
      json <- vaultKV.get[Json](path)
      result <- json.fold(ZIO.none)(json => ZIO.fromTry(ev.decodeValue(json)).asSome)
    } yield result
  }

  private def constructKeyPath[K, V](
      walletId: WalletId
  )(key: K)(implicit ev: GenericSecret[K, V]): (String, Map[String, String]) = {
    val basePath = s"${walletBasePath(walletId)}/generic-secrets"
    val relativePath = ev.keyPath(key)
    if (useSemanticPath) {
      s"$basePath/$relativePath" -> Map.empty
    } else {
      val relativePathHash = Sha256.compute(relativePath.getBytes(StandardCharsets.UTF_8)).getHexValue()
      s"$basePath/$relativePathHash" -> Map(SEMANTIC_PATH_METADATA_KEY -> relativePath)
    }
  }

}

object VaultGenericSecretStorage {
  def layer(useSemanticPath: Boolean): URLayer[VaultKVClient, GenericSecretStorage] =
    ZLayer.fromFunction(VaultGenericSecretStorage(_, useSemanticPath))
}
