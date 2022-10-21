package io.iohk.atala.iris.core.model.ledger

sealed trait BlockError extends Product with Serializable

object BlockError {

  case class NotFound(blockNo: Int) extends BlockError

  case object NoneAvailable extends BlockError
}
