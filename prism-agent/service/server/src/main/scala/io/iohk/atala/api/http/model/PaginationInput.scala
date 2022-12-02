package io.iohk.atala.api.http.model

import sttp.tapir.Codec.PlainCodec
import sttp.tapir.generic.auto.*
import sttp.tapir.{Codec, DecodeResult, Schema}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder}

import java.time.ZonedDateTime
import java.util.{Base64, UUID}
import scala.util.Try

case class PaginationInput(
    offset: Option[Int] = None,
    limit: Option[Int] = None
) {
  def toPagination = Pagination.apply(this)
}

case class Pagination(offset: Int, limit: Int) {
  def next = copy(offset = offset + limit)
  def prev = copy(offset = Math.max(offset - limit, 0))
}

object Pagination {
  def apply(in: PaginationInput): Pagination =
    Pagination(in.offset.getOrElse(0), in.limit.getOrElse(100))
}
