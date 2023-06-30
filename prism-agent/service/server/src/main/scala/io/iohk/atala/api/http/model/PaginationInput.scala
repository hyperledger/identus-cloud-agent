package io.iohk.atala.api.http.model

import sttp.tapir.EndpointIO.annotations.query
import sttp.tapir.Schema.annotations.validateEach
import sttp.tapir.Validator
import sttp.tapir.Schema

import io.iohk.atala.api.http.Annotation
case class PaginationInput(
    @query
    @validateEach(Validator.positiveOrZero[Int])
    offset: Option[Int] = None,
    @query
    @validateEach(Validator.positive[Int])
    limit: Option[Int] = None
) {
  def toPagination = Pagination.apply(this)
}

case class Pagination(offset: Int, limit: Int) {
  def next = copy(offset = offset + limit)
  def prev = copy(offset = Math.max(offset - limit, 0))
}

object Pagination {

  object annotations {
    object offset
        extends Annotation[Int](
          description = "The number of items to skip before returning results. Default is 0 if not specified",
          example = 0
        )

    object limit
        extends Annotation[Int](
          description = "The maximum number of items to return. Defaults to 100 if not specified.",
          example = 100
        )
  }

  def apply(in: PaginationInput): Pagination =
    Pagination(in.offset.getOrElse(0), in.limit.getOrElse(100))
}
