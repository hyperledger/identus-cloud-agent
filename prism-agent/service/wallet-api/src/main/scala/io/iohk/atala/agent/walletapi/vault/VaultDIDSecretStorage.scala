package io.iohk.atala.agent.walletapi.vault

import io.iohk.atala.agent.walletapi.storage.DIDSecret
import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage
import io.iohk.atala.mercury.model.DidId
import zio.*
import zio.json.*
import zio.json.ast.Json
import zio.json.ast.Json.*

import scala.util.Failure
import scala.util.Try

class VaultDIDSecretStorage(vaultKV: VaultKVClient) extends DIDSecretStorage {

  private val WALLET_PATH_PREFIX = "secret/default" // static <tenant-id> in signle-tenant mode

  override def insertKey(did: DidId, keyId: String, didSecret: DIDSecret): Task[Int] = {
    val path = s"$WALLET_PATH_PREFIX/peer-dids/${did.value}/$keyId"
    val kv = encodeJson(didSecret)
    for {
      alreadyExist <- vaultKV.get(path).map(_.isDefined)
      _ <- vaultKV
        .set(path, kv)
        .when(!alreadyExist)
        .someOrFail(Exception(s"Secret on path $path already exists."))
    } yield 1
  }

  override def getKey(did: DidId, keyId: String, schemaId: String): Task[Option[DIDSecret]] = {
    val path = s"$WALLET_PATH_PREFIX/peer-dids/${did.value}/$keyId"
    vaultKV.get(path).flatMap {
      case Some(kv) => ZIO.fromTry(decodeJson(kv, schemaId)).asSome
      case None     => ZIO.none
    }
  }

  private def encodeJson(didSecret: DIDSecret): Map[String, String] = {
    Map(didSecret.schemaId -> didSecret.json.toString())
  }

  private def decodeJson(kv: Map[String, String], schemaId: String): Try[DIDSecret] = {
    kv.get(schemaId) match {
      case Some(json) =>
        json.fromJson[Json].left.map(new RuntimeException(_)).toTry.map(json => DIDSecret(json, schemaId))
      case None => Failure(Exception(s"A property '$schemaId' is missing from KV data"))
    }
  }
}

object VaultDIDSecretStorage {
  def layer: URLayer[VaultKVClient, DIDSecretStorage] = ZLayer.fromFunction(VaultDIDSecretStorage(_))
}
