package io.iohk.atala.pollux.schema.controller

import io.iohk.atala.api.http.RequestContext
import io.iohk.atala.api.http.model.{CollectionStats, Pagination}
import io.iohk.atala.pollux.core.model
import io.iohk.atala.pollux.schema.model.{VerificationPolicy, VerificationPolicyPage}
import sttp.model.Uri
import sttp.model.Uri.QuerySegment
import sttp.model.Uri.QuerySegment.KeyValue

import scala.util.Try

//TODO:It's a hard copy of SchemaRegistryController. These two classes will be refactored later
//TODO: add nonce to the verification policy object
//TODO: fix self on creation of the self uri
case class VerificationPolicyPageRequestLogic(
    ctx: RequestContext,
    pagination: Pagination,
    items: List[VerificationPolicy],
    stats: CollectionStats
) {
  def composeNextUri(uri: Uri): Option[Uri] = if (
    stats.filteredCount == 0 || // no filtered content to return
    items.length < pagination.limit || // it's already the last page
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

  def result: VerificationPolicyPage = {
    val self = ctx.request.uri.toString
    val pageOf = ctx.request.uri.copy(querySegments = Seq.empty).toString
    val next = composeNextUri(ctx.request.uri).map(_.toString)
    val previous = composePreviousUri(ctx.request.uri).map(_.toString)
    val baseUri = ctx.request.uri.copy(querySegments = Seq.empty)

    val pageResult = VerificationPolicyPage(
      self = self,
      kind = "VerificationPolicyPage",
      pageOf = pageOf,
      next = next,
      previous = previous,
      contents = items.map(item => item.withBaseUri(baseUri))
    )

    pageResult
  }
}
