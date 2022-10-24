package io.iohk.atala.pollux.sql.repository

import doobie.*
import doobie.implicits.*
import io.iohk.atala.pollux.core.model.JWTCredential
import io.iohk.atala.pollux.core.repository.CredentialRepository
import io.iohk.atala.pollux.sql.model.JWTCredentialRow
import zio.*
import zio.interop.catz.*

// TODO: replace with actual implementation
class JdbcCredentialRepository(xa: Transactor[Task]) extends CredentialRepository[Task] {

  override def createCredentials(batchId: String, credentials: Seq[JWTCredential]): Task[Unit] = {
    ZIO.succeed(())
  }
  override def getCredentials(batchId: String): Task[Seq[JWTCredential]] = {
    ???
//    val cxnIO = sql"""
//         | SELECT
//         |   c.batch_id
//         |   c.credential_id
//         |   c.value
//         | FROM public.jwt_credentials AS c
//         | WHERE c.batch_id = $batchId
//         """.stripMargin
//      .query[JWTCredentialRow]
//      .to[Seq]
//
//    cxnIO
//      .transact(xa)
//      .map(_.map(c => JWTCredential(c.batchId, c.credentialId, c.content)))
  }

}

object JdbcCredentialRepository {
  val layer: URLayer[Transactor[Task], CredentialRepository[Task]] =
    ZLayer.fromFunction(new JdbcCredentialRepository(_))
}
