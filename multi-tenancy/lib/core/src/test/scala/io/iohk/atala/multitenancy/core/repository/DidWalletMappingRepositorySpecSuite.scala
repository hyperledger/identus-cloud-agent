package io.iohk.atala.multitenancy.core.repository

import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.multitenancy.core.model.*
import io.iohk.atala.multitenancy.core.model.error.DidWalletMappingRepositoryError.UniqueConstraintViolation
import io.iohk.atala.multitenancy.core.model.error.DidWalletMappingServiceError.*
import io.iohk.atala.shared.models.WalletId
import zio.test.*
import zio.{Cause, Exit, Task, ZIO}

import java.time.Instant
import java.util.UUID

object DidWalletMappingRepositorySpecSuite {

  private def didWalletMappingRecord = DidWalletMappingRecord(
    DidId("did:prism:aaa"),
    WalletId.random,
    Instant.ofEpochSecond(Instant.now.getEpochSecond),
    None
  )

  val testSuite = suite("CRUD operations")(
    test("createDidWalletMappingRecord creates a new record in DB") {
      for {
        repo <- ZIO.service[DidWalletMappingRepository[Task]]
        record = didWalletMappingRecord
        count <- repo.createDidWalletMappingRecord(record)
      } yield assertTrue(count == 1)
    },
    test("createDidWalletMappingRecord prevents creation of 2 records with the same did") {
      for {
        repo <- ZIO.service[DidWalletMappingRepository[Task]]
        thid = UUID.randomUUID().toString
        aRecord = didWalletMappingRecord.copy(did = DidId("did:prism:aaa"))
        bRecord = didWalletMappingRecord.copy(did = DidId("did:prism:aaa"))
        aCount <- repo.createDidWalletMappingRecord(aRecord)
        bCount <- repo.createDidWalletMappingRecord(bRecord).exit
      } yield {
        assertTrue(bCount match
          case Exit.Failure(cause: Cause.Fail[_]) if cause.value.isInstanceOf[UniqueConstraintViolation] => true
          case _                                                                                         => false
        )
      }
    },
    test("createDidWalletMappingRecord allows creation of 2 records with the same walletId") {
      for {
        repo <- ZIO.service[DidWalletMappingRepository[Task]]
        aRecord = didWalletMappingRecord
        bRecord = didWalletMappingRecord.copy(walletId = aRecord.walletId, did = DidId("did:prism:bbb"))
        _ <- repo.createDidWalletMappingRecord(aRecord)
        _ <- repo.createDidWalletMappingRecord(bRecord)
        records <- repo.getDidWalletMappingByWalletId(aRecord.walletId)
      } yield {
        assertTrue(records.size == 2) &&
        assertTrue(records.contains(aRecord)) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("getDidWalletMappingRecord correctly returns an existing record") {
      for {
        repo <- ZIO.service[DidWalletMappingRepository[Task]]
        aRecord = didWalletMappingRecord
        bRecord = didWalletMappingRecord.copy(did = DidId("did:prism:bbb"))
        _ <- repo.createDidWalletMappingRecord(aRecord)
        _ <- repo.createDidWalletMappingRecord(bRecord)
        record <- repo.getDidWalletMappingByDid(bRecord.did)
      } yield assertTrue(record.contains(bRecord))
    },
    test("getDidWalletMappingRecord returns None for an unknown did") {
      for {
        repo <- ZIO.service[DidWalletMappingRepository[Task]]
        aRecord = didWalletMappingRecord
        bRecord = didWalletMappingRecord.copy(did = DidId("did:prism:bbb"))
        _ <- repo.createDidWalletMappingRecord(aRecord)
        _ <- repo.createDidWalletMappingRecord(bRecord)
        record <- repo.getDidWalletMappingByDid(DidId("did:prism:unknown"))
      } yield assertTrue(record.isEmpty)
    },
    test("getDidWalletMappingRecord returns all records") {
      for {
        repo <- ZIO.service[DidWalletMappingRepository[Task]]
        aRecord = didWalletMappingRecord
        bRecord = didWalletMappingRecord.copy(did = DidId("did:prism:bbb"))
        _ <- repo.createDidWalletMappingRecord(aRecord)
        _ <- repo.createDidWalletMappingRecord(bRecord)
        records <- repo.getDidWalletMappingRecords
      } yield {
        assertTrue(records.size == 2) &&
        assertTrue(records.contains(aRecord)) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("getDidWalletMappingByDid returns correct record") {
      for {
        repo <- ZIO.service[DidWalletMappingRepository[Task]]
        aRecord = didWalletMappingRecord
        bRecord = didWalletMappingRecord.copy(did = DidId("did:prism:bbb"))
        cRecord = didWalletMappingRecord.copy(did = DidId("did:prism:ccc"))
        _ <- repo.createDidWalletMappingRecord(aRecord)
        _ <- repo.createDidWalletMappingRecord(bRecord)
        _ <- repo.createDidWalletMappingRecord(cRecord)
        record <- repo.getDidWalletMappingByDid(DidId("did:prism:ccc"))

      } yield {
        assertTrue(record.nonEmpty) &&
        assertTrue(record.contains(cRecord))
      }
    },
    test("getDidWalletMappingByWalletId returns correct record") {
      for {
        repo <- ZIO.service[DidWalletMappingRepository[Task]]
        aRecord = didWalletMappingRecord
        bRecord = didWalletMappingRecord.copy(did = DidId("did:prism:bbb"))
        cRecord = didWalletMappingRecord.copy(did = DidId("did:prism:ccc"))
        _ <- repo.createDidWalletMappingRecord(aRecord)
        _ <- repo.createDidWalletMappingRecord(bRecord)
        _ <- repo.createDidWalletMappingRecord(cRecord)
        record <- repo.getDidWalletMappingByWalletId(cRecord.walletId)

      } yield {
        assertTrue(record.nonEmpty) &&
        assertTrue(record.contains(cRecord))
      }
    },
    test("deleteRecord deletes an existing record by did") {
      for {
        repo <- ZIO.service[DidWalletMappingRepository[Task]]
        aRecord = didWalletMappingRecord
        bRecord = didWalletMappingRecord.copy(did = DidId("did:prism:bbb"))
        _ <- repo.createDidWalletMappingRecord(aRecord)
        _ <- repo.createDidWalletMappingRecord(bRecord)

        count <- repo.deleteDidWalletMappingByDid(aRecord.did)
        records <- repo.getDidWalletMappingRecords
      } yield {
        assertTrue(count == 1) &&
        assertTrue(records.size == 1) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("deleteRecord does nothing for an unknown record") {
      for {
        repo <- ZIO.service[DidWalletMappingRepository[Task]]
        aRecord = didWalletMappingRecord
        bRecord = didWalletMappingRecord.copy(did = DidId("did:prism:bbb"))
        cRecord = didWalletMappingRecord.copy(did = DidId("did:prism:ccc"))

        _ <- repo.createDidWalletMappingRecord(aRecord)
        _ <- repo.createDidWalletMappingRecord(bRecord)
        count <- repo.deleteDidWalletMappingByDid(cRecord.did)
        records <- repo.getDidWalletMappingRecords
      } yield {
        assertTrue(count == 0) &&
        assertTrue(records.size == 2) &&
        assertTrue(records.contains(aRecord)) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("deleteRecord deletes an existing record by walletId") {
      for {
        repo <- ZIO.service[DidWalletMappingRepository[Task]]
        aRecord = didWalletMappingRecord
        bRecord = didWalletMappingRecord.copy(did = DidId("did:prism:bbb"))
        cRecord = aRecord.copy(did = DidId("did:prism:ccc"))

        _ <- repo.createDidWalletMappingRecord(aRecord)
        _ <- repo.createDidWalletMappingRecord(bRecord)
        _ <- repo.createDidWalletMappingRecord(cRecord)

        count <- repo.deleteDidWalletMappingByWalletId(aRecord.walletId)
        records <- repo.getDidWalletMappingRecords
      } yield {
        assertTrue(count == 2) &&
        assertTrue(records.size == 1) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("deleteRecord does nothing for an unknown walletId") {
      for {
        repo <- ZIO.service[DidWalletMappingRepository[Task]]
        aRecord = didWalletMappingRecord
        bRecord = didWalletMappingRecord.copy(did = DidId("did:prism:bbb"))
        cRecord = didWalletMappingRecord.copy(did = DidId("did:prism:ccc"))

        _ <- repo.createDidWalletMappingRecord(aRecord)
        _ <- repo.createDidWalletMappingRecord(bRecord)
        count <- repo.deleteDidWalletMappingByWalletId(cRecord.walletId)
        records <- repo.getDidWalletMappingRecords
      } yield {
        assertTrue(count == 0) &&
        assertTrue(records.size == 2) &&
        assertTrue(records.contains(aRecord)) &&
        assertTrue(records.contains(bRecord))
      }
    }
  )
}
