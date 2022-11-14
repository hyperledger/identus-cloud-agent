package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.EncodedJWTCredential
import zio.*
import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import java.util.UUID
import io.iohk.atala.mercury.protocol.issuecredential.RequestCredential
import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
trait CredentialRepository[F[_]] {
  def createIssueCredentialRecord(record: IssueCredentialRecord): F[Int]

  def getIssueCredentialRecords(): F[Seq[IssueCredentialRecord]]

  def getIssueCredentialRecord(id: UUID): F[Option[IssueCredentialRecord]]

  def getIssueCredentialRecordByThreadId(id: UUID): F[Option[IssueCredentialRecord]]

  def updateCredentialRecordProtocolState(
      id: UUID,
      from: IssueCredentialRecord.ProtocolState,
      to: IssueCredentialRecord.ProtocolState
  ): F[Int]
  def updateCredentialRecordPublicationState(
      id: UUID,
      from: Option[IssueCredentialRecord.PublicationState],
      to: Option[IssueCredentialRecord.PublicationState]
  ): F[Int]

  def updateWithRequestCredential(request: RequestCredential): F[Int]

  def updateWithIssueCredential(issue: IssueCredential): F[Int]
}
