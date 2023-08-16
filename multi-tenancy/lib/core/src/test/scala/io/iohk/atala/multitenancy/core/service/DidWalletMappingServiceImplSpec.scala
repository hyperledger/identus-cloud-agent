package io.iohk.atala.multitenancy.core.service

import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.multitenancy.core.model.*
import io.iohk.atala.multitenancy.core.model.error.*
import io.iohk.atala.multitenancy.core.repository.DidWalletMappingRepositoryInMemory
import io.iohk.atala.shared.models.WalletId
import zio.*
import zio.test.*
import zio.test.Assertion.*

object DidWalletMappingServiceImplSpec extends ZIOSpecDefault {

  val didWalletMappingServiceLayer = DidWalletMappingRepositoryInMemory.layer >>> DidWalletMappingServiceImpl.layer

  override def spec = {
    suite("DidWalletMappingServiceImpl")(
      test("createDidWalletMappingRecord creates a valid did wallet id mapping record") {
        for {
          svc <- ZIO.service[DidWalletMappingService]
          did = DidId("did:peer:aaa")
          walletId = WalletId.random
          record <- svc.createDidWalletMapping(did, walletId)
        } yield {
          assertTrue(record.did == did) &&
          assertTrue(record.walletId == walletId)
        }
      }, {
        test("getDidWalletMappingRecords correctly returns all records") {
          for {
            svc <- ZIO.service[DidWalletMappingService]
            did = DidId("did:peer:aaa")
            walletId = WalletId.random
            createdRecord1 <- svc.createDidWalletMapping(did, walletId)
            did2 = DidId("did:peer:bbb")
            walletId2 = WalletId.random
            createdRecord2 <- svc.createDidWalletMapping(did2, walletId2)
            records <- svc.getDidWalletMappingRecords
          } yield {
            assertTrue(records.size == 2) &&
            assertTrue(records.contains(createdRecord1)) &&
            assertTrue(records.contains(createdRecord2))
          }
        }
      }, {
        test("getDidWalletMappingByDid correctly returns the record") {
          for {
            svc <- ZIO.service[DidWalletMappingService]
            did = DidId("did:peer:aaa")
            walletId = WalletId.random
            createdRecord1 <- svc.createDidWalletMapping(did, walletId)
            did2 = DidId("did:peer:bbb")
            walletId2 = WalletId.random
            createdRecord2 <- svc.createDidWalletMapping(did2, walletId2)
            record <- svc.getDidWalletMappingByDid(did)
          } yield {
            assertTrue(record.nonEmpty) &&
            assertTrue(record.contains(createdRecord1))
          }
        }
      }, {
        test("getDidWalletMappingByWalletId correctly returns all records") {
          for {
            svc <- ZIO.service[DidWalletMappingService]
            did = DidId("did:peer:aaa")
            walletId = WalletId.random
            createdRecord1 <- svc.createDidWalletMapping(did, walletId)
            did2 = DidId("did:peer:bbb")
            walletId2 = WalletId.random
            createdRecord2 <- svc.createDidWalletMapping(did2, walletId2)
            record <- svc.getDidWalletMappingByWalletId(walletId)
          } yield {
            assertTrue(record.nonEmpty) &&
            assertTrue(record.contains(createdRecord1))
          }
        }
      }
    ).provideLayer(didWalletMappingServiceLayer)
  }

}
