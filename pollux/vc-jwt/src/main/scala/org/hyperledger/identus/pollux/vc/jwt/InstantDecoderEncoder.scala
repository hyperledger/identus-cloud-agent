package org.hyperledger.identus.pollux.vc.jwt

import io.circe.Decoder
import io.circe.Encoder
import io.circe.HCursor

import java.time.Instant

object InstantDecoderEncoder {
  implicit val instantToEpochSecondsEncoder: Encoder[Instant] =
    (instant: Instant) => Encoder.encodeLong(instant.getEpochSecond)

  implicit val epochSecondsToInstantDecoder: Decoder[Instant] =
    (c: HCursor) => Decoder.decodeLong.map(s => Instant.ofEpochSecond(s)).apply(c)
}
