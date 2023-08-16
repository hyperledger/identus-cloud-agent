package io.iohk.atala.pollux.core.repository

import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
import io.iohk.atala.mercury.protocol.issuecredential.RequestCredential
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.ProtocolState
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import zio.*

trait CredentialRepository {
  def createIssueCredentialRecord(record: IssueCredentialRecord): Task[Int]
  def getIssueCredentialRecords(
      ignoreWithZeroRetries: Boolean = true,
      offset: Option[Int] = None,
      limit: Option[Int] = None
  ): Task[(Seq[IssueCredentialRecord], Int)]
  def getIssueCredentialRecord(recordId: DidCommID): Task[Option[IssueCredentialRecord]]
  def getIssueCredentialRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: IssueCredentialRecord.ProtocolState*
  ): Task[Seq[IssueCredentialRecord]]
  def updateCredentialRecordStateAndProofByCredentialIdBulk(
      idsStatesAndProofs: Seq[(DidCommID, IssueCredentialRecord.PublicationState, MerkleInclusionProof)]
  ): Task[Int]

  def getIssueCredentialRecordByThreadId(
      thid: DidCommID,
      ignoreWithZeroRetries: Boolean = true,
  ): Task[Option[IssueCredentialRecord]]

  def updateCredentialRecordProtocolState(
      recordId: DidCommID,
      from: IssueCredentialRecord.ProtocolState,
      to: IssueCredentialRecord.ProtocolState
  ): Task[Int]

  def updateCredentialRecordPublicationState(
      recordId: DidCommID,
      from: Option[IssueCredentialRecord.PublicationState],
      to: Option[IssueCredentialRecord.PublicationState]
  ): Task[Int]

  def updateWithSubjectId(
      recordId: DidCommID,
      subjectId: String,
      protocolState: ProtocolState
  ): Task[Int]

  def updateWithRequestCredential(
      recordId: DidCommID,
      request: RequestCredential,
      protocolState: ProtocolState
  ): Task[Int]

  def updateWithIssueCredential(
      recordId: DidCommID,
      issue: IssueCredential,
      protocolState: ProtocolState
  ): Task[Int]

  def updateWithIssuedRawCredential(
      recordId: DidCommID,
      issue: IssueCredential,
      issuedRawCredential: String,
      protocolState: ProtocolState
  ): Task[Int]

  def deleteIssueCredentialRecord(recordId: DidCommID): Task[Int]

  def getValidIssuedCredentials(recordId: Seq[DidCommID]): Task[Seq[ValidIssuedCredentialRecord]]

  def updateAfterFail(
      recordId: DidCommID,
      failReason: Option[String]
  ): Task[Int]

}
