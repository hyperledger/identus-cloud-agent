package io.iohk.atala.pollux.core.repository

import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
import io.iohk.atala.mercury.protocol.issuecredential.RequestCredential
import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.ProtocolState
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.PublicationState
import io.iohk.atala.pollux.core.model.ValidIssuedCredentialRecord
import io.iohk.atala.pollux.core.model.error.CredentialRepositoryError._
import io.iohk.atala.prism.crypto.MerkleInclusionProof
import zio.*

import java.time.Instant
import java.util.UUID

class CredentialRepositoryInMemory(storeRef: Ref[Map[UUID, IssueCredentialRecord]]) extends CredentialRepository[Task] {

  override def updateCredentialRecordPublicationState(
      recordId: UUID,
      from: Option[PublicationState],
      to: Option[PublicationState]
  ): Task[Int] = {
    for {
      store <- storeRef.get
      maybeRecord = store.find((uuid, record) => uuid == recordId && record.publicationState == from).map(_._2)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r => r.updated(recordId, record.copy(publicationState = to)))
          } yield 1
        )
        .getOrElse(ZIO.succeed(0))
    } yield count
  }

  override def createIssueCredentialRecord(record: IssueCredentialRecord): Task[Int] = {
    for {
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

  override def getIssueCredentialRecord(recordId: UUID): Task[Option[IssueCredentialRecord]] = {
    for {
      store <- storeRef.get
      record = store.get(recordId)
    } yield record
  }

  override def getIssueCredentialRecords(): Task[Seq[IssueCredentialRecord]] = {
    for {
      store <- storeRef.get
    } yield store.values.toSeq
  }

  override def updateCredentialRecordProtocolState(
      recordId: UUID,
      from: ProtocolState,
      to: ProtocolState
  ): Task[Int] = {
    for {
      store <- storeRef.get
      maybeRecord = store.find((uuid, record) => uuid == recordId && record.protocolState == from).map(_._2)
      count <- maybeRecord
        .map(record =>
          for {
            _ <- storeRef.update(r => r.updated(recordId, record.copy(protocolState = to)))
          } yield 1
        )
        .getOrElse(ZIO.succeed(0))
    } yield count
  }

  override def updateWithIssuedRawCredential(
      recordId: UUID,
      issue: IssueCredential,
      issuedRawCredential: String,
      protocolState: ProtocolState
  ): Task[Int] = {
    for {
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
                  protocolState = protocolState
                )
              )
            )
          } yield 1
        )
        .getOrElse(ZIO.succeed(1))
    } yield count
  }

  override def getValidIssuedCredentials(recordId: Seq[UUID]): Task[Seq[ValidIssuedCredentialRecord]] = {
    for {
      store <- storeRef.get
    } yield store.values
      .filter(rec => recordId.contains(rec.id) && rec.issuedCredentialRaw.isDefined)
      .map(rec => ValidIssuedCredentialRecord(rec.id, rec.issuedCredentialRaw))
      .toSeq
  }

  override def deleteIssueCredentialRecord(recordId: UUID): Task[Int] = {
    for {
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
      recordId: UUID,
      issue: IssueCredential,
      protocolState: ProtocolState
  ): Task[Int] = {
    for {
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
                  protocolState = protocolState
                )
              )
            )
          } yield 1
        )
        .getOrElse(ZIO.succeed(1))
    } yield count
  }

  override def getIssueCredentialRecordsByState(state: ProtocolState): Task[Seq[IssueCredentialRecord]] = {
    for {
      store <- storeRef.get
    } yield store.values.filter(rec => rec.protocolState == state).toSeq
  }

  override def getIssueCredentialRecordByThreadId(thid: UUID): Task[Option[IssueCredentialRecord]] = {
    for {
      store <- storeRef.get
    } yield store.values.find(_.thid == thid)
  }

  override def updateWithRequestCredential(
      recordId: UUID,
      request: RequestCredential,
      protocolState: ProtocolState
  ): Task[Int] = {
    for {
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
                  protocolState = protocolState
                )
              )
            )
          } yield 1
        )
        .getOrElse(ZIO.succeed(1))
    } yield count
  }

  override def updateCredentialRecordStateAndProofByCredentialIdBulk(
      idsStatesAndProofs: Seq[(UUID, PublicationState, MerkleInclusionProof)]
  ): Task[Int] = ???

}

object CredentialRepositoryInMemory {
  val layer: ULayer[CredentialRepository[Task]] = ZLayer.fromZIO(
    Ref
      .make(Map.empty[UUID, IssueCredentialRecord])
      .map(CredentialRepositoryInMemory(_))
  )
}
