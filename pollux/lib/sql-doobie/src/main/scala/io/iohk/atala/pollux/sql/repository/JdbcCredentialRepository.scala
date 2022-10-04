package io.iohk.atala.pollux.sql.repository

import doobie.*
import doobie.implicits.*
import io.iohk.atala.pollux.core.model.W3CCredential
import io.iohk.atala.pollux.core.repository.CredentialRepository
import zio.*
import zio.interop.catz.*

// TODO: replace with actual implementation
class JdbcCredentialRepository(xa: Transactor[Task]) extends CredentialRepository[Task] {

  override def getCredentials: Task[Seq[W3CCredential]] = {
    val cxnIO = sql"""
         |SELECT foo FROM public.published_did_operations
         |""".stripMargin.query[String].to[Seq]

    cxnIO
      .transact(xa)
      .map(_.map(W3CCredential.apply))
  }

}

object JdbcCredentialRepository {
  val layer: URLayer[Transactor[Task], CredentialRepository[Task]] =
    ZLayer.fromFunction(new JdbcCredentialRepository(_))
}
