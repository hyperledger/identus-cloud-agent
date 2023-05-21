package io.iohk.atala.castor.core.repository

import io.iohk.atala.castor.core.model.did.PrismDID

trait DIDOperationRepository[F[_]] {
  def getConfirmedPublishedDIDOperations(did: PrismDID): F[Unit]
}
