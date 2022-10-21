package io.iohk.atala.mercury.model

import io.circe._
import io.circe.generic.semiauto._
import scala.util.Try

final case class DidId(value: String)
object DidId {
  given encoder: Encoder[DidId] = Encoder.encodeString.contramap[DidId](_.toString)
  given decoder: Decoder[DidId] = Decoder.decodeString.emapTry { str => Try(DidId(str)) }
}
