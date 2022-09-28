package io.iohk.atala.agent.sql.repository

import doobie.*
import doobie.implicits.*
import io.iohk.atala.agent.core.model.PublishedDIDOperation
import io.iohk.atala.agent.core.repository.DIDOperationRepository
import zio.*
import zio.interop.catz.*

// TODO: replace with actual implementation
class JdbcDIDOperationRepository(xa: Transactor[Task]) extends DIDOperationRepository[Task] {

  override def getPublishedOperations: Task[Seq[PublishedDIDOperation]] = {
    val cxnIO = sql"""
         |SELECT foo FROM public.published_did_operations
         |""".stripMargin.query[String].to[Seq]

    cxnIO
      .transact(xa)
      .map(_.map(PublishedDIDOperation.apply))
  }

}

object JdbcDIDOperationRepository {
  val layer: URLayer[Transactor[Task], DIDOperationRepository[Task]] =
    ZLayer.fromFunction(new JdbcDIDOperationRepository(_))
}
