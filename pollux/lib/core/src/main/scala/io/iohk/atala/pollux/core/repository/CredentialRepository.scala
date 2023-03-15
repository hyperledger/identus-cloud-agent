package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.EncodedJWTCredential
import io.iohk.atala.pollux.core.model._
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import zio.*

import io.iohk.atala.mercury.protocol.issuecredential.RequestCredential
import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.ProtocolState

trait CredentialRepository[F[_]] {
  def createIssueCredentialRecord(record: IssueCredentialRecord): F[Int]
  def getIssueCredentialRecords(
      igoneWithZeroRetries: Boolean = true
  ): F[Seq[IssueCredentialRecord]]
  def getIssueCredentialRecord(recordId: DidCommID): F[Option[IssueCredentialRecord]]
  def getIssueCredentialRecordsByStates(
      igoneWithZeroRetries: Boolean = true,
      states: IssueCredentialRecord.ProtocolState*
  ): F[Seq[IssueCredentialRecord]]
  def updateCredentialRecordStateAndProofByCredentialIdBulk(
      idsStatesAndProofs: Seq[(DidCommID, IssueCredentialRecord.PublicationState, MerkleInclusionProof)]
  ): F[Int]

  def getIssueCredentialRecordByThreadId(thid: DidCommID): F[Option[IssueCredentialRecord]]

  def updateCredentialRecordProtocolState(
      recordId: DidCommID,
      from: IssueCredentialRecord.ProtocolState,
      to: IssueCredentialRecord.ProtocolState
  ): F[Int]

  def updateCredentialRecordPublicationState(
      recordId: DidCommID,
      from: Option[IssueCredentialRecord.PublicationState],
      to: Option[IssueCredentialRecord.PublicationState]
  ): F[Int]

  def updateWithSubjectId(
      recordId: DidCommID,
      subjectId: String,
      protocolState: ProtocolState
  ): F[Int]

  def updateWithRequestCredential(
      recordId: DidCommID,
      request: RequestCredential,
      protocolState: ProtocolState
  ): F[Int]

  def updateWithIssueCredential(
      recordId: DidCommID,
      issue: IssueCredential,
      protocolState: ProtocolState
  ): F[Int]

  def updateWithIssuedRawCredential(
      recordId: DidCommID,
      issue: IssueCredential,
      issuedRawCredential: String,
      protocolState: ProtocolState
  ): F[Int]

  def deleteIssueCredentialRecord(recordId: DidCommID): F[Int]

  def getValidIssuedCredentials(recordId: Seq[DidCommID]): F[Seq[ValidIssuedCredentialRecord]]

  def updateAfterFail(
      recordId: DidCommID,
      failReason: Option[String]
  ): Task[Int]

}
