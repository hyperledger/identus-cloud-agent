package io.iohk.atala.iris.core.repository

import io.iohk.atala.iris.core.model as model
import zio.*

// TODO: replace with actual implementation
trait OperationsRepository[F[_]] {
  def getOperation(id: model.IrisOperationId): F[model.IrisOperation]
  def saveOperations(ops: Seq[model.IrisOperation]): F[Unit]
}
