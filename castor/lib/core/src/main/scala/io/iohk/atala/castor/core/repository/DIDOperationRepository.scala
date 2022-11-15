package io.iohk.atala.castor.core.repository

import io.iohk.atala.castor.core.model.did.{ConfirmedPublishedDIDOperation, PrismDID}
import zio.*

trait DIDOperationRepository[F[_]] {
  def getConfirmedPublishedDIDOperations(did: PrismDID): F[Seq[ConfirmedPublishedDIDOperation]]
}
