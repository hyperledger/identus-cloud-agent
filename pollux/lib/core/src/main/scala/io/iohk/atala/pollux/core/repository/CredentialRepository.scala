package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.EncodedJWTCredential
import zio.*
import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import java.util.UUID
import io.iohk.atala.mercury.protocol.issuecredential.RequestCredential
import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
trait CredentialRepository[F[_]] {
  def createCredentials(batchId: String, credentials: Seq[EncodedJWTCredential]): F[Unit]
  def getCredentials(batchId: String): F[Seq[EncodedJWTCredential]]
  def createIssueCredentialRecord(record: IssueCredentialRecord): F[Int]
  def getIssueCredentialRecords(): F[Seq[IssueCredentialRecord]]
  def getIssueCredentialRecord(id: UUID): F[Option[IssueCredentialRecord]]
  def updateCredentialRecordState(id: UUID, from: IssueCredentialRecord.State, to: IssueCredentialRecord.State): F[Int]
  def updateWithRequestCredential(request: RequestCredential): F[Int]
  def updateWithIssueCredential(issue: IssueCredential): F[Int]
}
