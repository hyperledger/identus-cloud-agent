package io.iohk.atala.agent.walletapi.sql

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import io.iohk.atala.agent.walletapi.model.{DIDUpdateLineage, ManagedDIDState}
import io.iohk.atala.agent.walletapi.storage.DIDNonSecretStorage
import io.iohk.atala.castor.core.model.did.{PrismDID, ScheduledDIDOperationStatus}
import zio.*
import zio.interop.catz.*

// TODO: implement missing members
class JdbcDIDNonSecretStorage(xa: Transactor[Task]) extends DIDNonSecretStorage {

  override def getManagedDIDState(did: PrismDID): Task[Option[ManagedDIDState]] = {
    val cxnIO =
      sql"""
        | SELECT
        |   did,
        |   publication_status,
        |   atala_operation_content,
        |   publish_operation_id,
        |   created_at,
        |   updated_at
        | FROM public.prism_did_wallet_state
        | WHERE did = $did
        """.stripMargin
        .query[DIDPublicationStateRow]
        .option

    cxnIO
      .transact(xa)
      .flatMap(_.map(_.toDomain).fold(ZIO.none)(t => ZIO.fromTry(t).asSome))
  }

  override def setManagedDIDState(did: PrismDID, state: ManagedDIDState): Task[Unit] = {
    val cxnIO = (row: DIDPublicationStateRow) => sql"""
        | INSERT INTO public.prism_did_wallet_state(
        |   did,
        |   publication_status,
        |   atala_operation_content,
        |   publish_operation_id,
        |   created_at,
        |   updated_at
        | )
        | VALUES (
        |   ${row.did},
        |   ${row.publicationStatus},
        |   ${row.atalaOperationContent},
        |   ${row.publishOperationId},
        |   ${row.createdAt},
        |   ${row.updatedAt}
        | )
        | ON CONFLICT (did) DO UPDATE SET
        |   publication_status = EXCLUDED.publication_status,
        |   atala_operation_content = EXCLUDED.atala_operation_content,
        |   publish_operation_id = EXCLUDED.publish_operation_id,
        |   updated_at = EXCLUDED.updated_at
        """.stripMargin.update

    for {
      now <- Clock.instant
      row = DIDPublicationStateRow.from(did, state, now)
      _ <- cxnIO(row).run.transact(xa).unit.tapErrorCause(e => ZIO.logErrorCause(e)) // TODO: remove
    } yield ()
  }

  override def listManagedDID: Task[Map[PrismDID, ManagedDIDState]] = {
    val cxnIO =
      sql"""
           | SELECT
           |   did,
           |   publication_status,
           |   atala_operation_content,
           |   publish_operation_id,
           |   created_at,
           |   updated_at
           | FROM public.prism_did_wallet_state
           """.stripMargin
        .query[DIDPublicationStateRow]
        .to[List]

    cxnIO
      .transact(xa)
      .map(_.map(row => row.toDomain.map(row.did -> _)))
      .flatMap(ls => ZIO.foreach(ls)(ZIO.fromTry[(PrismDID, ManagedDIDState)](_)))
      .map(_.toMap)
  }

  override def insertUpdateLineage(did: PrismDID, updateLineage: DIDUpdateLineage): Task[Unit] = ???

  override def listUpdateLineage(did: PrismDID): Task[Seq[DIDUpdateLineage]] = ???

  override def setUpdateLineageStatus(operationHash: Array[Byte], status: ScheduledDIDOperationStatus): Task[Unit] = ???

}

object JdbcDIDNonSecretStorage {
  val layer: URLayer[Transactor[Task], DIDNonSecretStorage] = ZLayer.fromFunction(new JdbcDIDNonSecretStorage(_))
}
