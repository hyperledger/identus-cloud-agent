package org.hyperledger.identus.pollux.core.repository

import org.hyperledger.identus.mercury.protocol.issuecredential.{IssueCredential, RequestCredential}
import org.hyperledger.identus.pollux.anoncreds.AnoncredCredentialRequestMetadata
import org.hyperledger.identus.pollux.core.model.*
import org.hyperledger.identus.pollux.core.model.IssueCredentialRecord.ProtocolState
import org.hyperledger.identus.shared.models.WalletAccessContext
import zio.*

trait CredentialRepository {
  def create(
      record: IssueCredentialRecord
  ): RIO[WalletAccessContext, Int]

  def findAll(
      ignoreWithZeroRetries: Boolean,
      offset: Option[Int] = None,
      limit: Option[Int] = None
  ): RIO[WalletAccessContext, (Seq[IssueCredentialRecord], Int)]

  def getById(
      recordId: DidCommID
  ): RIO[WalletAccessContext, IssueCredentialRecord]

  def findById(
      recordId: DidCommID
  ): RIO[WalletAccessContext, Option[IssueCredentialRecord]]

  def findByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: IssueCredentialRecord.ProtocolState*
  ): RIO[WalletAccessContext, Seq[IssueCredentialRecord]]

  def findByStatesForAllWallets(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: IssueCredentialRecord.ProtocolState*
  ): Task[Seq[IssueCredentialRecord]]

  def findByThreadId(
      thid: DidCommID,
      ignoreWithZeroRetries: Boolean,
  ): RIO[WalletAccessContext, Option[IssueCredentialRecord]]

  def updateProtocolState(
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

  def deleteById(
      recordId: DidCommID
  ): RIO[WalletAccessContext, Int]

  def findValidIssuedCredentials(
      recordId: Seq[DidCommID]
  ): RIO[WalletAccessContext, Seq[ValidIssuedCredentialRecord]]

  def findValidAnonCredsIssuedCredentials(
      recordIds: Seq[DidCommID]
  ): RIO[WalletAccessContext, Seq[ValidFullIssuedCredentialRecord]]

  def updateAfterFail(
      recordId: DidCommID,
      failReason: Option[String]
  ): RIO[WalletAccessContext, Int]

}
