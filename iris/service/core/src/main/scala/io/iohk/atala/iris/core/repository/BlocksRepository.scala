package io.iohk.atala.iris.core.repository

import io.iohk.atala.iris.core.model.ledger.BlockError
import io.iohk.atala.iris.core.model.ledger.Block

trait ROBlocksRepository[F[_]] {
  def getFullBlock(blockNo: Int): F[Either[BlockError.NotFound, Block.Full]]
  def getLatestBlock: F[Either[BlockError.NoneAvailable.type, Block.Canonical]]
}
