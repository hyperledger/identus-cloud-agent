package io.iohk.atala.castor.sql.repository

import doobie.*
import doobie.implicits.*
import io.iohk.atala.castor.core.model.IrisNotification
import io.iohk.atala.castor.core.repository.DIDOperationRepository
import zio.*
import zio.interop.catz.*

// TODO: replace with actual implementation
class JdbcDIDOperationRepository(xa: Transactor[Task]) extends DIDOperationRepository[Task] {

  override def getIrisNotification: Task[Seq[IrisNotification]] = {
    val cxnIO = sql"""
         |SELECT foo FROM public.published_did_operations
         |""".stripMargin.query[String].to[Seq]

    cxnIO
      .transact(xa)
      .map(_.map(IrisNotification.apply))
  }

}

object JdbcDIDOperationRepository {
  val layer: URLayer[Transactor[Task], DIDOperationRepository[Task]] =
    ZLayer.fromFunction(new JdbcDIDOperationRepository(_))
}
