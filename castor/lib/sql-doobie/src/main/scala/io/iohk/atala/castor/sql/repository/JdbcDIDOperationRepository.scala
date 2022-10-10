package io.iohk.atala.castor.sql.repository

import doobie.*
import doobie.implicits.*
import io.iohk.atala.castor.core.model.IrisNotification
import io.iohk.atala.castor.core.model.did.ConfirmedPublishedDIDOperation
import io.iohk.atala.castor.core.repository.DIDOperationRepository
import io.iohk.atala.shared.models.HexStrings.HexString
import zio.*
import zio.interop.catz.*

class JdbcDIDOperationRepository(xa: Transactor[Task]) extends DIDOperationRepository[Task] {

  // TODO: implement
  override def getConfirmedPublishedDIDOperations(didSuffix: HexString): Task[Seq[ConfirmedPublishedDIDOperation]] = {
    ZIO.succeed(Nil)
  }

}

object JdbcDIDOperationRepository {
  val layer: URLayer[Transactor[Task], DIDOperationRepository[Task]] =
    ZLayer.fromFunction(new JdbcDIDOperationRepository(_))
}
