package org.hyperledger.identus.mercury.model

import io.circe.*
import io.circe.generic.semiauto.*

import scala.util.Try

final case class DidId(value: String)
object DidId {
  given encoder: Encoder[DidId] = Encoder.encodeString.contramap[DidId](_.value)
  given decoder: Decoder[DidId] = Decoder.decodeString.emapTry { str => Try(DidId(str)) }
}
