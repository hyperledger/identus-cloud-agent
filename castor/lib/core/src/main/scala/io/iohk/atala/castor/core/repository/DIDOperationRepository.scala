package io.iohk.atala.castor.core.repository

import io.iohk.atala.castor.core.model.did.{ConfirmedPublishedDIDOperation, PrismDIDV1}
import zio.*

trait DIDOperationRepository[F[_]] {
  def getConfirmedPublishedDIDOperations(did: PrismDIDV1): F[Seq[ConfirmedPublishedDIDOperation]]
}
