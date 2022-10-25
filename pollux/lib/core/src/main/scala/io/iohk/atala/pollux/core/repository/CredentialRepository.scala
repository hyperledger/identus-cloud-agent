package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.JWTCredential
import zio.*
import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import java.util.UUID
trait CredentialRepository[F[_]] {
  def createCredentials(batchId: String, credentials: Seq[JWTCredential]): F[Unit]
  def getCredentials(batchId: String): F[Seq[JWTCredential]]
  def createIssueCredentialRecord(record: IssueCredentialRecord): F[Unit]
  def getIssueCredentialRecords(): F[Seq[IssueCredentialRecord]]
  def getIssueCredentialRecord(id: UUID): F[Option[IssueCredentialRecord]]
}
