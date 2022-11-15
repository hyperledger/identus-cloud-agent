package io.iohk.atala.castor.sql.repository

import doobie.*
import doobie.implicits.*
import io.iohk.atala.castor.core.model.did.{ConfirmedPublishedDIDOperation, PrismDID}
import io.iohk.atala.castor.core.repository.DIDOperationRepository
import io.iohk.atala.castor.sql.model.SqlModelHelper
import io.iohk.atala.castor.sql.repository.Utils.connectionIOSafe
import io.iohk.atala.castor.sql.repository.dao.DIDOperationDAO
import io.iohk.atala.shared.models.HexStrings.HexString
import io.iohk.atala.shared.utils.Traverse.*
import zio.*
import zio.interop.catz.*

class JdbcDIDOperationRepository(xa: Transactor[Task]) extends DIDOperationRepository[Task], SqlModelHelper {

  override def getConfirmedPublishedDIDOperations(did: PrismDID): Task[Seq[ConfirmedPublishedDIDOperation]] = {
    val query = DIDOperationDAO.getConfirmedPublishedDIDOperation(did.asCanonical.suffix.value)
    connectionIOSafe(query)
      .transact(xa)
      .absolve
      .map(_.traverse(_.toDomain).left.map(Exception(_)))
      .absolve
  }

}

object JdbcDIDOperationRepository {
  val layer: URLayer[Transactor[Task], DIDOperationRepository[Task]] =
    ZLayer.fromFunction(new JdbcDIDOperationRepository(_))
}
