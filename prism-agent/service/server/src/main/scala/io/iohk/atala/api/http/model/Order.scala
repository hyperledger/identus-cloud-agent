package io.iohk.atala.api.http.model

import sttp.tapir.Codec.PlainCodec
import sttp.tapir.generic.auto.*
import sttp.tapir.{Codec, DecodeResult, Schema}

import java.util.Base64

case class Order(field: String, direction: Option[Order.Direction] = None)

object Order {
  val DefaultDirection = Direction.Ascending
  val empty = Order("")

  enum Direction(kind: String):
    case Ascending extends Direction("asc")
    case Descending extends Direction("desc")

  import io.iohk.atala.api.http.codec.OrderCodec.orderCodec
}
