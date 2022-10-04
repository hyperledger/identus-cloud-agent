package io.iohk.atala.castor.core.repository

import io.iohk.atala.castor.core.model.PublishedDIDOperation
import zio.*

// TODO: replace with actual implementation
trait DIDOperationRepository[F[_]] {
  def getPublishedOperations: F[Seq[PublishedDIDOperation]]
}
