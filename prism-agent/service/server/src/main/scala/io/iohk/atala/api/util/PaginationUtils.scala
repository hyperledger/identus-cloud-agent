package io.iohk.atala.api.util

import io.iohk.atala.api.http.model.Pagination
import io.iohk.atala.api.http.model.CollectionStats
import scala.util.chaining.scalaUtilChainingOps

object PaginationUtils {

  def composePageOfUri[U: UriUpdate](uri: U): U = {
    val uriUpdate = summon[UriUpdate[U]]
    uri
      .pipe(uriUpdate.removeAllQueryParam(_, "offset"))
      .pipe(uriUpdate.removeAllQueryParam(_, "limit"))
  }

  def composeNextUri[U: UriUpdate](
      uri: U,
      items: Seq[Any],
      pagination: Pagination,
      stats: CollectionStats
  ): Option[U] = {
    val hasNoItem = stats.filteredCount == 0
    val hasNoMoreItem = items.length < pagination.limit
    val isOnLastPage = (pagination.offset + pagination.limit) == stats.filteredCount

    if (hasNoItem || hasNoMoreItem || isOnLastPage) None
    else {
      val next = pagination.next
      val uriUpdate = summon[UriUpdate[U]]
      Some(
        uri
          .pipe(uriUpdate.removeAllQueryParam(_, "offset"))
          .pipe(uriUpdate.removeAllQueryParam(_, "limit"))
          .pipe(uriUpdate.addQueryParam(_, "offset", next.offset.toString))
          .pipe(uriUpdate.addQueryParam(_, "limit", pagination.limit.toString))
      )
    }
  }

  def composePreviousUri[U: UriUpdate](
      uri: U,
      items: Seq[Any],
      pagination: Pagination,
      stats: CollectionStats
  ): Option[U] = {
    val hasNoItem = stats.filteredCount == 0
    val isOnFirstPage = pagination.offset == 0

    if (hasNoItem || isOnFirstPage) None
    else {
      val prev = pagination.prev
      val uriUpdate = summon[UriUpdate[U]]
      Some(
        uri
          .pipe(uriUpdate.removeAllQueryParam(_, "offset"))
          .pipe(uriUpdate.removeAllQueryParam(_, "limit"))
          .pipe(uriUpdate.addQueryParam(_, "offset", prev.offset.toString))
          .pipe(uriUpdate.addQueryParam(_, "limit", pagination.limit.toString))
      )
    }
  }

}

trait UriUpdate[U] {
  def addQueryParam(uri: U, key: String, value: String): U
  def removeAllQueryParam(uri: U, key: String): U
}

object UriUpdate {
  type SttpUri = sttp.model.Uri
  type AkkaUri = akka.http.scaladsl.model.Uri

  given UriUpdate[SttpUri] with {
    override def addQueryParam(uri: SttpUri, key: String, value: String): SttpUri =
      uri.addParam(key, value)
    override def removeAllQueryParam(uri: SttpUri, key: String): SttpUri = {
      val filteredQuerySegments = uri.querySegments.filterNot {
        case sttp.model.Uri.QuerySegment.KeyValue(k, _, _, _) => k == key
        case _                                                => false
      }
      uri.copy(querySegments = filteredQuerySegments)
    }
  }

  given UriUpdate[AkkaUri] with {
    override def addQueryParam(uri: AkkaUri, key: String, value: String): AkkaUri =
      uri.withQuery((key, value) +: uri.query())
    override def removeAllQueryParam(uri: AkkaUri, key: String): AkkaUri =
      uri.withQuery(uri.query().filterNot { case (k, _) => k == key })
  }
}
