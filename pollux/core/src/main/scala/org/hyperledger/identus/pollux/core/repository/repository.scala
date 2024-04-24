package org.hyperledger.identus.pollux.core.repository

import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.RIO

trait Repository[F[_], T]

type WalletTask[T] = RIO[WalletAccessContext, T]

object Repository {
  case class SearchQuery[Filter](filter: Filter, skip: Int, limit: Int)

  case class SearchResult[T](entries: Seq[T], count: Long, totalCount: Long)

  trait SearchCapability[F[_], Filter, T] {
    self: Repository[F, T] =>
    def search(query: SearchQuery[Filter]): F[SearchResult[T]]
  }
}
