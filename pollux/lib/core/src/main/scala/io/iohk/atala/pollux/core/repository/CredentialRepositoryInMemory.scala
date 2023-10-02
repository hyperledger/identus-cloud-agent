package io.iohk.atala.pollux.core.repository

import io.iohk.atala.mercury.protocol.issuecredential.{IssueCredential, RequestCredential}
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.{ProtocolState, PublicationState}
import io.iohk.atala.pollux.core.model.error.CredentialRepositoryError.*
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import zio.*

import java.time.Instant

class CredentialRepositoryInMemory(
    walletRefs: Ref[Map[WalletId, Ref[Map[DidCommID, IssueCredentialRecord]]]],
    maxRetries: Int
) extends CredentialRepository {

  private def walletStoreRef: URIO[WalletAccessContext, Ref[Map[DidCommID, IssueCredentialRecord]]] =
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      refs <- walletRefs.get
      maybeWalletRef = refs.get(walletId)
      walletRef <- maybeWalletRef
        .fold {
          for {
            ref <- Ref.make(Map.empty[DidCommID, IssueCredentialRecord])
            _ <- walletRefs.set(refs.updated(walletId, ref))
          } yield ref
        }(ZIO.succeed)
    } yield walletRef

  override def updateCredentialRecordPublicationState(
      recordId: DidCommID,
      from: Option[PublicationState],
      to: Option[PublicationState]
  ): RIO[WalletAccessContext, Int] = {
    for {
      storeRef <- walletStoreRef
      store <- storeRef.get
      maybeRecord = store.find((id, record) => id == recordId && record.publicationState == from).map(_._2)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r => r.updated(recordId, record.copy(publicationState = to)))
          } yield 1
        )
        .getOrElse(ZIO.succeed(0))
    } yield count
  }

  override def createIssueCredentialRecord(record: IssueCredentialRecord): RIO[WalletAccessContext, Int] = {
    for {
      storeRef <- walletStoreRef
      _ <- for {
        store <- storeRef.get
        maybeRecord = store.values.find(_.thid == record.thid)
        _ <- maybeRecord match
          case None        => ZIO.unit
          case Some(value) => ZIO.fail(UniqueConstraintViolation("Unique Constraint Violation on 'thid'"))
      } yield ()
      _ <- storeRef.update(r => r + (record.id -> record))
    } yield 1
  }

  override def getIssueCredentialRecord(
      recordId: DidCommID
  ): RIO[WalletAccessContext, Option[IssueCredentialRecord]] = {
    for {
      storeRef <- walletStoreRef
      store <- storeRef.get
      record = store.get(recordId)
    } yield record
  }

  override def getIssueCredentialRecords(
      ignoreWithZeroRetries: Boolean,
      offset: Option[Int],
      limit: Option[Int]
  ): RIO[WalletAccessContext, (Seq[IssueCredentialRecord], Int)] = {
    for {
      storeRef <- walletStoreRef
      store <- storeRef.get
      records = if (ignoreWithZeroRetries) store.values.filter(_.metaRetries > 0) else store.values
      paginated = records.toSeq.drop(offset.getOrElse(0)).take(limit.getOrElse(Int.MaxValue))
    } yield paginated -> store.values.size
  }

  override def updateCredentialRecordProtocolState(
      recordId: DidCommID,
      from: ProtocolState,
      to: ProtocolState
  ): RIO[WalletAccessContext, Int] = {
    for {
      storeRef <- walletStoreRef
      store <- storeRef.get
      maybeRecord = store.find((id, record) => id == recordId && record.protocolState == from).map(_._2)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                recordId,
                record.copy(
                  protocolState = to,
                  metaRetries = maxRetries,
                  metaLastFailure = None,
                )
              )
            )
          } yield 1
        )
        .getOrElse(ZIO.succeed(0))
    } yield count
  }

  override def updateWithIssuedRawCredential(
      recordId: DidCommID,
      issue: IssueCredential,
      issuedRawCredential: String,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int] = {
    for {
      storeRef <- walletStoreRef
      maybeRecord <- getIssueCredentialRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                recordId,
                record.copy(
                  updatedAt = Some(Instant.now),
                  issueCredentialData = Some(issue),
                  issuedCredentialRaw = Some(issuedRawCredential),
                  protocolState = protocolState,
                  metaRetries = maxRetries,
                  metaLastFailure = None,
                )
              )
            )
          } yield 1
        )
        .getOrElse(ZIO.succeed(1))
    } yield count
  }

  override def getValidIssuedCredentials(
      recordId: Seq[DidCommID]
  ): RIO[WalletAccessContext, Seq[ValidIssuedCredentialRecord]] = {
    for {
      storeRef <- walletStoreRef
      store <- storeRef.get
    } yield store.values
      .filter(rec => recordId.contains(rec.id) && rec.issuedCredentialRaw.isDefined)
      .map(rec => ValidIssuedCredentialRecord(rec.id, rec.issuedCredentialRaw, rec.subjectId))
      .toSeq
  }

  override def deleteIssueCredentialRecord(recordId: DidCommID): RIO[WalletAccessContext, Int] = {
    for {
      storeRef <- walletStoreRef
      maybeRecord <- getIssueCredentialRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r => r.removed(recordId))
          } yield 1
        )
        .getOrElse(ZIO.succeed(0))
    } yield count
  }

  override def updateWithIssueCredential(
      recordId: DidCommID,
      issue: IssueCredential,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int] = {
    for {
      storeRef <- walletStoreRef
      maybeRecord <- getIssueCredentialRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                recordId,
                record.copy(
                  updatedAt = Some(Instant.now),
                  issueCredentialData = Some(issue),
                  protocolState = protocolState,
                  metaRetries = maxRetries,
                  metaLastFailure = None,
                )
              )
            )
          } yield 1
        )
        .getOrElse(ZIO.succeed(1))
    } yield count
  }

  override def getIssueCredentialRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      states: ProtocolState*
  ): RIO[WalletAccessContext, Seq[IssueCredentialRecord]] = {
    for {
      storeRef <- walletStoreRef
      store <- storeRef.get
      records = if (ignoreWithZeroRetries) store.values.filter(_.metaRetries > 0) else store.values
    } yield records
      .filter(rec => states.contains(rec.protocolState))
      .take(limit)
      .toSeq
  }

  override def getIssueCredentialRecordByThreadId(
      thid: DidCommID,
      ignoreWithZeroRetries: Boolean,
  ): RIO[WalletAccessContext, Option[IssueCredentialRecord]] = {
    for {
      storeRef <- walletStoreRef
      store <- storeRef.get
      records = if (ignoreWithZeroRetries) store.values.filter(_.metaRetries > 0) else store.values
    } yield records.find(_.thid == thid)
  }

  override def updateWithSubjectId(
      recordId: DidCommID,
      subjectId: String,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int] = {
    for {
      storeRef <- walletStoreRef
      maybeRecord <- getIssueCredentialRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                recordId,
                record.copy(
                  updatedAt = Some(Instant.now),
                  protocolState = protocolState,
                  subjectId = Some(subjectId),
                  metaRetries = maxRetries,
                  metaLastFailure = None,
                )
              )
            )
          } yield 1
        )
        .getOrElse(ZIO.succeed(0))
    } yield count
  }

  override def updateWithRequestCredential(
      recordId: DidCommID,
      request: RequestCredential,
      protocolState: ProtocolState
  ): RIO[WalletAccessContext, Int] = {
    for {
      storeRef <- walletStoreRef
      maybeRecord <- getIssueCredentialRecord(recordId)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r =>
              r.updated(
                recordId,
                record.copy(
                  updatedAt = Some(Instant.now),
                  requestCredentialData = Some(request),
                  protocolState = protocolState,
                  metaRetries = maxRetries,
                  metaLastFailure = None,
                )
              )
            )
          } yield 1
        )
        .getOrElse(ZIO.succeed(1))
    } yield count
  }

  override def updateCredentialRecordStateAndProofByCredentialIdBulk(
      idsStatesAndProofs: Seq[(DidCommID, PublicationState, MerkleInclusionProof)]
  ): RIO[WalletAccessContext, Int] = ???

  def updateAfterFail(
      recordId: DidCommID,
      failReason: Option[String]
  ): RIO[WalletAccessContext, Int] = for {
    storeRef <- walletStoreRef
    maybeRecord <- getIssueCredentialRecord(recordId)
    count <- maybeRecord
      .map(record =>
        for {
          _ <- storeRef.update(r =>
            r.updated(
              recordId,
              record.copy(
                metaRetries = math.max(0, record.metaRetries - 1),
                metaNextRetry =
                  if (record.metaRetries - 1 <= 0) None
                  else Some(Instant.now().plusSeconds(60)), // TODO exponention time
                metaLastFailure = failReason
              )
            )
          )
        } yield 1
      )
      .getOrElse(ZIO.succeed(0))
  } yield count

}

object CredentialRepositoryInMemory {
  val maxRetries = 5 // TODO Move to config
  val layer: ULayer[CredentialRepository] = ZLayer.fromZIO(
    Ref
      .make(Map.empty[WalletId, Ref[Map[DidCommID, IssueCredentialRecord]]])
      .map(CredentialRepositoryInMemory(_, maxRetries))
  )
}
