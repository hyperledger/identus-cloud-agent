package io.iohk.atala.connect.core.repository

import zio.*
import java.util.UUID
import io.iohk.atala.mercury.protocol.issuecredential.RequestCredential
import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
io.iohk.atala.connect.core.model.ConnectionsRecord
trait ConnectionsRepository[F[_]] {
  def createInvitationRecord(record: ConnectionsRecord): F[Int]

  def getIInvitationRecords(): F[Seq[ConnectionsRecord]]

  def getIssueCredentialRecord(id: UUID): F[Option[ConnectionsRecord]]

  def getIssueCredentialRecordByThreadId(id: UUID): F[Option[ConnectionsRecord]]

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
