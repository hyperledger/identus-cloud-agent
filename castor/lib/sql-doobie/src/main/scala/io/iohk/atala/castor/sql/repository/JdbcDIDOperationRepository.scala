package io.iohk.atala.castor.sql.repository

import doobie.*
import doobie.implicits.*
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.castor.core.repository.DIDOperationRepository
import io.iohk.atala.castor.sql.repository.Utils.connectionIOSafe
import io.iohk.atala.shared.models.HexStrings.HexString
import io.iohk.atala.shared.utils.Traverse.*
import zio.*
import zio.interop.catz.*

class JdbcDIDOperationRepository(xa: Transactor[Task]) extends DIDOperationRepository[Task] {

  override def getConfirmedPublishedDIDOperations(did: PrismDID): Task[Unit] = ZIO.unit

}

object JdbcDIDOperationRepository {
  val layer: URLayer[Transactor[Task], DIDOperationRepository[Task]] =
    ZLayer.fromFunction(new JdbcDIDOperationRepository(_))
}
