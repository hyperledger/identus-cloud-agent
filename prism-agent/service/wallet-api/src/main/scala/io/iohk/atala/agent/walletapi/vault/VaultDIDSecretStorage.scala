package io.iohk.atala.agent.walletapi.vault

import com.nimbusds.jose.jwk.OctetKeyPair
import io.iohk.atala.agent.walletapi.storage.DIDSecretStorage
import io.iohk.atala.mercury.model.DidId
import scala.util.Try
import zio.*
import scala.util.Failure

class VaultDIDSecretStorage(vaultKV: VaultKVClient) extends DIDSecretStorage {

  private val WALLET_PATH_PREFIX = "secret/default" // static <tenant-id> in signle-tenant mode

  override def insertKey(did: DidId, keyId: String, keyPair: OctetKeyPair): Task[Int] = {
    val path = s"$WALLET_PATH_PREFIX/peer-dids/${did.value}/$keyId"
    val kv = encodeOctetKeyPair(keyPair)
    for {
      alreadyExist <- vaultKV.get(path).map(_.isDefined)
      _ <- vaultKV
        .set(path, kv)
        .when(!alreadyExist)
        .someOrFail(Exception(s"Secret on path $path alraedy exists."))
    } yield 1
  }

  override def getKey(did: DidId, keyId: String): Task[Option[OctetKeyPair]] = {
    val path = s"$WALLET_PATH_PREFIX/peer-dids/${did.value}/$keyId"
    vaultKV.get(path).flatMap {
      case Some(kv) => ZIO.fromTry(decodeOctetKeyPair(kv)).asSome
      case None     => ZIO.none
    }
  }

  private def encodeOctetKeyPair(keyPair: OctetKeyPair): Map[String, String] = {
    Map("jwk" -> keyPair.toJSONString())
  }

  private def decodeOctetKeyPair(kv: Map[String, String]): Try[OctetKeyPair] = {
    kv.get("jwk") match {
      case Some(jwk) => Try(OctetKeyPair.parse(jwk))
      case None      => Failure(Exception("A property 'jwk' is missing from KV data"))
    }
  }
}

object VaultDIDSecretStorage {
  def layer: URLayer[VaultKVClient, DIDSecretStorage] = ZLayer.fromFunction(VaultDIDSecretStorage(_))
}
