package io.iohk.atala.pollux.schema.controller

import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.{CollectionStats, Pagination}
import io.iohk.atala.pollux.schema.model.VerifiableCredentialSchemaPage
import sttp.model.Uri
import sttp.model.Uri.QuerySegment
import sttp.model.Uri.QuerySegment.KeyValue

import scala.util.Try

case class SchemaRegistryController(
    ctx: RequestContext,
    pagination: Pagination,
    page: VerifiableCredentialSchemaPage,
    stats: CollectionStats
) {
  def composeNextUri(uri: Uri): Option[Uri] = if (
    stats.filteredCount == 0 || // no filtered content to return
    page.contents.length < pagination.limit || // it's already the last page
    pagination.offset + pagination.limit == stats.filteredCount // it's exactly the last page
  ) None
  else {
    val next = pagination.next
    val newOffsetQueryParam = KeyValue(k = "offset", v = next.offset.toString)
    val newLimitQueryParam = KeyValue("limit", pagination.limit.toString)
    Some(
      uri.copy(
        querySegments = dropQueryParam(uri.querySegments, Set("limit", "offset")) ++
          Seq(newOffsetQueryParam, newLimitQueryParam)
      )
    )
  }

  def composePreviousUri(uri: Uri): Option[Uri] = if (
    pagination.offset == 0 || // it's the beginning of the pagination
    stats.filteredCount == 0 // no filtered content to return
  ) None
  else {
    val prev = pagination.prev
    val newOffsetQueryParam = KeyValue(k = "offset", v = prev.offset.toString)
    val newLimitQueryParam = KeyValue(k = "limit", v = prev.limit.toString)
    Some(
      uri.copy(
        querySegments = dropQueryParam(uri.querySegments, Set("limit", "offset")) ++
          Seq(newOffsetQueryParam, newLimitQueryParam)
      )
    )
  }

  def dropQueryParam(seq: Seq[QuerySegment], keysToDrop: Set[String]) =
    seq.filterNot {
      case KeyValue(k, _, _, _) => keysToDrop(k)
      case _                    => false
    }

  def result: VerifiableCredentialSchemaPage = {
    val self = ctx.request.uri.toString
    val pageOf = ctx.request.uri.copy(querySegments = Seq.empty).toString
    val next = composeNextUri(ctx.request.uri).map(_.toString)
    val previous = composePreviousUri(ctx.request.uri).map(_.toString)

    val pageResult = page.copy(
      self = self,
      pageOf = pageOf,
      next = next,
      previous = previous,
      contents = page.contents.map(item =>
        item.withBaseUri(
          ctx.request.uri.copy(querySegments = Seq.empty)
        )
      )
    )

    pageResult
  }
}
