package io.iohk.atala.iris.sql.repository

import doobie.*
import doobie.implicits.*
import io.iohk.atala.iris.core.repository.DbRepositoryTransactor
import zio.*
import zio.interop.catz.*

class JdbcDbRepositoryTransactorIO(xa: Transactor[Task]) extends DbRepositoryTransactor[ConnectionIO] {
  override def runAtomically[A](action: ConnectionIO[A]): Task[A] = action.transact(xa)
}

object JdbcDbRepositoryTransactorIO {
  val layer: URLayer[Transactor[Task], JdbcDbRepositoryTransactorIO] =
    ZLayer.fromFunction(new JdbcDbRepositoryTransactorIO(_))
}
