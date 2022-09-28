package io.iohk.atala.agent.core.repository

import io.iohk.atala.agent.core.model.PublishedDIDOperation
import zio.*

// TODO: replace with actual implementation
trait DIDOperationRepository[F[_]] {
  def getPublishedOperations: F[Seq[PublishedDIDOperation]]
}
