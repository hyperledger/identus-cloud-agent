package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.EncodedJWTCredential
import io.iohk.atala.pollux.core.model._
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import zio.*

import java.util.UUID
import io.iohk.atala.mercury.protocol.issuecredential.RequestCredential
import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.ProtocolState

trait CredentialRepository[F[_]] {
  def createIssueCredentialRecord(record: IssueCredentialRecord): F[Int]
  def getIssueCredentialRecords(): F[Seq[IssueCredentialRecord]]
  def getIssueCredentialRecord(recordId: UUID): F[Option[IssueCredentialRecord]]
  def getIssueCredentialRecordsByState(state: IssueCredentialRecord.ProtocolState): F[Seq[IssueCredentialRecord]]
  def updateCredentialRecordStateAndProofByCredentialIdBulk(
      idsStatesAndProofs: Seq[(UUID, IssueCredentialRecord.PublicationState, MerkleInclusionProof)]
  ): F[Int]

  def getIssueCredentialRecordByThreadId(thid: UUID): F[Option[IssueCredentialRecord]]

  def updateCredentialRecordProtocolState(
      recordId: UUID,
      from: IssueCredentialRecord.ProtocolState,
      to: IssueCredentialRecord.ProtocolState
  ): F[Int]
  def updateCredentialRecordPublicationState(
      recordId: UUID,
      from: Option[IssueCredentialRecord.PublicationState],
      to: Option[IssueCredentialRecord.PublicationState]
  ): F[Int]

  def updateWithRequestCredential(recordId: UUID, request: RequestCredential, protocolState: ProtocolState): F[Int]

  def updateWithIssueCredential(
      recordId: UUID,
      issue: IssueCredential,
      protocolState: ProtocolState
  ): F[Int]

  def updateWithIssuedRawCredential(
      recordId: UUID,
      issue: IssueCredential,
      issuedRawCredential: String,
      protocolState: ProtocolState
  ): F[Int]

  def getValidIssuedCredentials(recordId: Seq[UUID]): F[Seq[ValidIssuedCredentialRecord]]

}
