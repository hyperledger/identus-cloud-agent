package io.iohk.atala.agent.walletapi

import com.nimbusds.jose.jwk.OctetKeyPair
import io.iohk.atala.agent.walletapi.model.WalletSeed
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
        case None => Failure(Exception("A property 'value' is missing from vault KV data"))
        case Some(encodedSeed) =>
          HexString
            .fromString(encodedSeed)
            .map(_.toByteArray)
            .flatMap(bytes => WalletSeed.fromByteArray(bytes).left.map(Exception(_)).toTry)
      }
    }
  }

  given KVCodec[OctetKeyPair] = new {
    override def encode(value: OctetKeyPair): Map[String, String] =
      Map("value" -> value.toJSONString())

    override def decode(kv: Map[String, String]): Try[OctetKeyPair] =
      for {
        jwk <- kv.get("value").toRight(Exception("A property 'value' is missing from vault KV data")).toTry
        keyPair <- Try(OctetKeyPair.parse(jwk))
      } yield keyPair
  }

  given KVCodec[Json] = new {
    override def encode(value: Json): Map[String, String] =
      Map("value" -> value.toJson)

    override def decode(kv: Map[String, String]): Try[Json] =
      for {
        json <- kv.get("value").toRight(Exception("A property 'value' is missing from vault KV data")).toTry
        keyPair <- json
          .fromJson[Json]
          .left
          .map(s => Exception(s"Fail to parse JSON from string: $s"))
          .toTry
      } yield keyPair
  }
}
