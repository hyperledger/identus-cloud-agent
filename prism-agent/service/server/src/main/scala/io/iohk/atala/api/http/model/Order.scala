package io.iohk.atala.api.http.model

case class Order(field: String, direction: Option[Order.Direction] = None)

object Order {
  val DefaultDirection: Direction = Direction.Ascending
  val empty: Order = Order("")

  enum Direction(kind: String):
    case Ascending extends Direction("asc")
    case Descending extends Direction("desc")
}
