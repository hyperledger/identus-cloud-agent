package io.iohk.atala.castor.core.repository

import io.iohk.atala.castor.core.model.IrisNotification
import zio.*

// TODO: replace with actual implementation
trait DIDOperationRepository[F[_]] {
  def getIrisNotification: F[Seq[IrisNotification]]
}
