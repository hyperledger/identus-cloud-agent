package io.iohk.atala.iris.core.repository

import io.iohk.atala.iris.core.model.{IrisOperation, IrisOperationId, SignedIrisOperation}
import zio.*

// TODO: replace with actual implementation
trait OperationsRepository[F[_]] {
  def getOperation(id: IrisOperationId): F[IrisOperation]
  def saveOperations(ops: Seq[SignedIrisOperation]): F[Unit]
}
