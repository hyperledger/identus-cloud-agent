package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.repository.Repository.{SearchQuery, SearchResult}

trait Repository[F[_], T]

object Repository {
  case class SearchQuery[Filter](filter: Filter, skip: Int, limit: Int)

  case class SearchResult[T](entries: Seq[T], count: Long, totalCount: Long)

  trait SearchCapability[F[_], Filter, T] {
    self: Repository[F, T] =>
    def search(query: SearchQuery[Filter]): F[SearchResult[T]]
  }
}
