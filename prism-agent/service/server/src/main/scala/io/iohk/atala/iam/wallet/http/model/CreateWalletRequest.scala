package io.iohk.atala.iam.wallet.http.model

import io.iohk.atala.api.http.Annotation
import sttp.tapir.*
import sttp.tapir.Schema.annotations.{description, encodedExample}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

final case class CreateWalletRequest(
    @description(CreateWalletRequest.annotations.seed.description)
    @encodedExample(CreateWalletRequest.annotations.seed.example)
    seed: Option[String]
)

object CreateWalletRequest {
  given encoder: JsonEncoder[CreateWalletRequest] = DeriveJsonEncoder.gen
  given decoder: JsonDecoder[CreateWalletRequest] = DeriveJsonDecoder.gen
  given schema: Schema[CreateWalletRequest] = Schema.derived

  object annotations {
    object seed
        extends Annotation[String](
          description =
            "A BIP32 seed encoded in hexadecimal string. It is expected to represent 64-bytes binary seed (128 hex characters).",
          example =
            "c9994785ce6d548134020f610b76102ca1075d3bb672a75ec8c9a27a7b8607e3b9b384e43b77bb08f8d5159651ae38b98573f7ecc79f2d7e1f1cc371ce60cf8a"
        )
  }
}
