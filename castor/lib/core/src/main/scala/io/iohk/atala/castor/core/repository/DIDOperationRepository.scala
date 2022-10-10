package io.iohk.atala.castor.core.repository

import io.iohk.atala.castor.core.model.IrisNotification
import io.iohk.atala.castor.core.model.did.ConfirmedPublishedDIDOperation
import io.iohk.atala.shared.models.HexStrings.HexString
import zio.*

// TODO: replace with actual implementation
trait DIDOperationRepository[F[_]] {
  def getConfirmedPublishedDIDOperations(didSuffix: HexString): F[Seq[ConfirmedPublishedDIDOperation]]
}
