package io.iohk.atala.agent.walletapi.sql

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*

import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.agent.walletapi.storage.DIDNonSecretStorage
import io.iohk.atala.castor.core.model.did.PrismDID
import zio.*
import zio.interop.catz.*

class JdbcDIDNonSecretStorage(xa: Transactor[Task]) extends DIDNonSecretStorage {

  override def getManagedDIDState(did: PrismDID): Task[Option[ManagedDIDState]] = {
    val cxnIO =
      sql"""
        | SELECT
        |   did,
        |   publication_status,
        |   create_operation,
        |   publish_operation_id
        | FROM public.did_publication_state
        | WHERE did = $did
        """.stripMargin
        .query[DIDPublicationStateRow]
        .option

    cxnIO
      .transact(xa)
      .flatMap(_.map(_.toDomain).fold(ZIO.none)(t => ZIO.fromTry(t).asSome))
  }

  override def setManagedDIDState(did: PrismDID, state: ManagedDIDState): Task[Unit] = {
    val row = DIDPublicationStateRow.from(did, state)
    val cxnIO = sql"""
        | INSERT INTO public.did_publication_state(
        |   did,
        |   publication_status,
        |   create_operation,
        |   publish_operation_id
        | )
        | VALUES (
        |   ${row.did},
        |   ${row.publicationStatus},
        |   ${row.createOperation},
        |   ${row.publishOperationId}
        | )
        | ON CONFLICT (did) DO UPDATE SET
        |   publication_status = EXCLUDED.publication_status,
        |   create_operation = EXCLUDED.create_operation,
        |   publish_operation_id = EXCLUDED.publish_operation_id
        """.stripMargin.update

    cxnIO.run.transact(xa).unit
  }

  override def listManagedDID: Task[Map[PrismDID, ManagedDIDState]] = {
    val cxnIO =
      sql"""
           | SELECT
           |   did,
           |   publication_status,
           |   create_operation,
           |   publish_operation_id
           | FROM public.did_publication_state
    """.stripMargin
        .query[DIDPublicationStateRow]
        .to[List]

    cxnIO
      .transact(xa)
      .map(_.map(row => row.toDomain.map(row.did -> _)))
      .flatMap(ls => ZIO.foreach(ls)(ZIO.fromTry[(PrismDID, ManagedDIDState)](_)))
      .map(_.toMap)
  }

}

object JdbcDIDNonSecretStorage {
  val layer: URLayer[Transactor[Task], DIDNonSecretStorage] = ZLayer.fromFunction(new JdbcDIDNonSecretStorage(_))
}
