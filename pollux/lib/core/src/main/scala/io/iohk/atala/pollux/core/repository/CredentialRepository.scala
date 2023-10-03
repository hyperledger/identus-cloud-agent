package io.iohk.atala.pollux.core.repository

import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
import io.iohk.atala.mercury.protocol.issuecredential.RequestCredential
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.ProtocolState
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

trait CredentialRepository {
  def createIssueCredentialRecord(record: IssueCredentialRecord): RIO[WalletAccessContext, Int]
  def getIssueCredentialRecords(
      ignoreWithZeroRetries: Boolean,
      offset: Option[Int] = None,
      limit: Option[Int] = None
  ): RIO[WalletAccessContext, (Seq[IssueCredentialRecord], Int)]
  def getIssueCredentialRecord(recordId: DidCommID): RIO[WalletAccessContext, Option[IssueCredentialRecord]]
  def getIssueCredentialRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: IssueCredentialRecord.ProtocolState*
  ): RIO[WalletAccessContext, Seq[IssueCredentialRecord]]
  def updateCredentialRecordStateAndProofByCredentialIdBulk(
      idsStatesAndProofs: Seq[(DidCommID, IssueCredentialRecord.PublicationState, MerkleInclusionProof)]
  ): RIO[WalletAccessContext, Int]

  def getIssueCredentialRecordByThreadId(
      thid: DidCommID,
      ignoreWithZeroRetries: Boolean,
  ): RIO[WalletAccessContext, Option[IssueCredentialRecord]]

  def updateCredentialRecordProtocolState(
      recordId: DidCommID,
      from: IssueCredentialRecord.ProtocolState,
      to: IssueCredentialRecord.ProtocolState
  ): RIO[WalletAccessContext, Int]

  def updateCredentialRecordPublicationState(
      recordId: DidCommID,
      from: Option[IssueCredentialRecord.PublicationState],
      to: Option[IssueCredentialRecord.PublicationState]
  ): RIO[WalletAccessContext, Int]

  def updateWithSubjectId(
      recordId: DidCommID,
      subjectId: String,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int]

  def updateWithRequestCredential(
      recordId: DidCommID,
      request: RequestCredential,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int]

  def updateWithIssueCredential(
      recordId: DidCommID,
      issue: IssueCredential,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int]

  def updateWithIssuedRawCredential(
      recordId: DidCommID,
      issue: IssueCredential,
      issuedRawCredential: String,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int]

  def deleteIssueCredentialRecord(recordId: DidCommID): RIO[WalletAccessContext, Int]

  def getValidIssuedCredentials(recordId: Seq[DidCommID]): RIO[WalletAccessContext, Seq[ValidIssuedCredentialRecord]]

  def updateAfterFail(
      recordId: DidCommID,
      failReason: Option[String]
  ): RIO[WalletAccessContext, Int]

}
