package io.iohk.atala.castor.sql.model

import io.iohk.atala.shared.models.HexStrings.HexString

import java.time.Instant

private[sql] final case class ConfirmedPublishedDIDOperationRow(
    ledgerName: String,
    didSuffix: HexString,
    operationType: OperationType,
    operationContent: Array[Byte],
    anchoredAt: Instant,
    blockNumber: Int,
    blockIndex: Int
)

private[sql] enum OperationType {
  case CREATE extends OperationType
  case UPDATE extends OperationType
  case RECOVER extends OperationType
  case DEACTIVATE extends OperationType
}
