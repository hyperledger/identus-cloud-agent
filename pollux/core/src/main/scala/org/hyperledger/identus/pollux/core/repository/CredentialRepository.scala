package org.hyperledger.identus.pollux.core.repository

import org.hyperledger.identus.mercury.protocol.issuecredential.{IssueCredential, RequestCredential}
import org.hyperledger.identus.pollux.anoncreds.AnoncredCredentialRequestMetadata
import org.hyperledger.identus.pollux.core.model.*
import org.hyperledger.identus.pollux.core.model.IssueCredentialRecord.ProtocolState
import org.hyperledger.identus.shared.models.WalletAccessContext
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

  def getIssueCredentialRecordsByStatesForAllWallets(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: IssueCredentialRecord.ProtocolState*
  ): Task[Seq[IssueCredentialRecord]]

  def getIssueCredentialRecordByThreadId(
      thid: DidCommID,
      ignoreWithZeroRetries: Boolean,
  ): RIO[WalletAccessContext, Option[IssueCredentialRecord]]

  def updateCredentialRecordProtocolState(
      recordId: DidCommID,
      from: IssueCredentialRecord.ProtocolState,
      to: IssueCredentialRecord.ProtocolState
  ): RIO[WalletAccessContext, Int]

  def updateWithSubjectId(
      recordId: DidCommID,
      subjectId: String,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int]

  def updateWithJWTRequestCredential(
      recordId: DidCommID,
      request: RequestCredential,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int]

  def updateWithAnonCredsRequestCredential(
      recordId: DidCommID,
      request: RequestCredential,
      metadata: AnoncredCredentialRequestMetadata,
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
      schemaUri: Option[String],
      credentialDefinitionUri: Option[String],
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int]

  def deleteIssueCredentialRecord(recordId: DidCommID): RIO[WalletAccessContext, Int]

  def getValidIssuedCredentials(recordId: Seq[DidCommID]): RIO[WalletAccessContext, Seq[ValidIssuedCredentialRecord]]

  def getValidAnoncredIssuedCredentials(
      recordIds: Seq[DidCommID]
  ): RIO[WalletAccessContext, Seq[ValidFullIssuedCredentialRecord]]

  def updateAfterFail(
      recordId: DidCommID,
      failReason: Option[String]
  ): RIO[WalletAccessContext, Int]

}
