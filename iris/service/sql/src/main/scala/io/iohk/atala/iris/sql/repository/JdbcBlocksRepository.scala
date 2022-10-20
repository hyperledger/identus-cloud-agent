package io.iohk.atala.iris.sql.repository

import doobie.Transactor
import io.iohk.atala.iris.core.model.ledger.{Block, BlockError}
import zio.*
import io.iohk.atala.iris.core.repository.ROBlocksRepository

class JdbcBlocksRepository(xa: Transactor[Task]) extends ROBlocksRepository[Task] {
  override def getFullBlock(blockNo: RuntimeFlags): Task[Either[BlockError.NotFound, Block.Full]] = ???

  override def getLatestBlock: Task[Either[BlockError.NoneAvailable.type, Block.Canonical]] = ???
}

object JdbcBlocksRepository {
  val layer: URLayer[Transactor[Task], ROBlocksRepository[Task]] =
    ZLayer.fromFunction(new JdbcBlocksRepository(_))
}
