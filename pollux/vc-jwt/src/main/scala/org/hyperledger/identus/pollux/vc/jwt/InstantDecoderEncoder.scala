package org.hyperledger.identus.pollux.vc.jwt

import io.circe.{Decoder, HCursor}
import zio.json.{JsonDecoder, JsonEncoder}

import java.time.Instant

object InstantDecoderEncoder {
  given JsonEncoder[Instant] = JsonEncoder.long.contramap(_.getEpochSecond)

  implicit val epochSecondsToInstantDecoder: Decoder[Instant] =
    (c: HCursor) => Decoder.decodeLong.map(s => Instant.ofEpochSecond(s)).apply(c)

  given JsonDecoder[Instant] = JsonDecoder.long.map(Instant.ofEpochSecond)
}
