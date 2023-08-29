package io.iohk.atala.agent.walletapi

import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.storage.DIDSecret
import io.iohk.atala.shared.models.HexString
import zio.json.*
import zio.json.ast.Json
import zio.json.ast.Json.*

import scala.util.Failure
import scala.util.Try

package object vault {
  trait KVCodec[T] {
    def encode(value: T): Map[String, String]
    def decode(kv: Map[String, String]): Try[T]
  }

  given KVCodec[WalletSeed] = new {
    override def encode(value: WalletSeed): Map[String, String] = {
      val bytes = value.toByteArray
      Map(
        "value" -> HexString.fromByteArray(bytes).toString
      )
    }

    override def decode(kv: Map[String, String]): Try[WalletSeed] = {
      kv.get("value") match {
        case None => Failure(Exception("A property 'value' is missing from KV data"))
        case Some(encodedSeed) =>
          HexString
            .fromString(encodedSeed)
            .map(_.toByteArray)
            .flatMap(bytes => WalletSeed.fromByteArray(bytes).left.map(Exception(_)).toTry)
      }
    }
  }

  given KVCodec[DIDSecret] = new {
    override def encode(value: DIDSecret): Map[String, String] = {
      Map(
        "schemaId" -> value.schemaId,
        "json" -> value.json.toString()
      )
    }

    override def decode(kv: Map[String, String]): Try[DIDSecret] = {
      for {
        schemaId <- kv.get("schemaId").toRight(Exception(s"A property 'schemaId' is missing from KV data")).toTry
        json <- kv
          .get("json")
          .toRight(Exception(s"A property 'json' is missing from KV data"))
          .flatMap { jsonStr => jsonStr.fromJson[Json].left.map(RuntimeException(_)) }
          .toTry
      } yield DIDSecret(json, schemaId)
    }
  }
}
