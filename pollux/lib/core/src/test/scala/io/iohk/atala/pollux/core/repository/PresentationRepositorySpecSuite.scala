package io.iohk.atala.pollux.core.repository

import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.presentproof.{Presentation, ProposePresentation, RequestPresentation}
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.PresentationRecord.*
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletId
import zio.test.*
import zio.test.Assertion.*
import zio.{ZIO, ZLayer}

import java.time.Instant
import java.util.UUID

object PresentationRepositorySpecSuite {
  val maxRetries = 5 // TODO Move to config

  private def presentationRecord = PresentationRecord(
    id = DidCommID(),
    createdAt = Instant.now,
    updatedAt = None,
    thid = DidCommID(),
    schemaId = None,
    connectionId = None,
    role = PresentationRecord.Role.Verifier,
    subjectId = DidId("did:prism:aaa"),
    protocolState = PresentationRecord.ProtocolState.RequestPending,
    requestPresentationData = None,
    proposePresentationData = None,
    presentationData = None,
    credentialsToUse = None,
    metaRetries = maxRetries,
    metaNextRetry = Some(Instant.now()),
    metaLastFailure = None,
  )

  private def requestPresentation = RequestPresentation(
    from = DidId("did:prism:aaa"),
    to = DidId("did:prism:bbb"),
    thid = Some(UUID.randomUUID.toString),
    body = RequestPresentation.Body(goal_code = Some("request Presentation")),
    attachments = Nil
  )

  private def proposePresentation = ProposePresentation(
    from = DidId("did:prism:aaa"),
    to = DidId("did:prism:bbb"),
    thid = Some(UUID.randomUUID.toString),
    body = ProposePresentation.Body(goal_code = Some("Propose Presentation")),
    attachments = Nil
  )

  val testSuite = suite("CRUD operations")(
    test("createPresentationRecord creates a new record in DB") {
      for {
        repo <- ZIO.service[PresentationRepository]
        record = presentationRecord
        count <- repo.createPresentationRecord(record)
      } yield assertTrue(count == 1)
    },
    test("createPresentationRecord correctly read and write on non-null connectionId") {
      for {
        repo <- ZIO.service[PresentationRepository]
        record = presentationRecord.copy(connectionId = Some("connectionId"))
        count <- repo.createPresentationRecord(record)
        readRecord <- repo.getPresentationRecord(record.id)
      } yield assertTrue(count == 1) && assert(readRecord)(isSome(equalTo(record)))
    },
    test("getPresentationRecord correctly returns an existing record") {
      for {
        repo <- ZIO.service[PresentationRepository]
        aRecord = presentationRecord
        bRecord = presentationRecord
        _ <- repo.createPresentationRecord(aRecord)
        _ <- repo.createPresentationRecord(bRecord)
        record <- repo.getPresentationRecord(bRecord.id)
      } yield assertTrue(record.contains(bRecord))
    },
    test("getPresentationRecord returns None for an unknown record") {
      for {
        repo <- ZIO.service[PresentationRepository]
        aRecord = presentationRecord
        bRecord = presentationRecord
        _ <- repo.createPresentationRecord(aRecord)
        _ <- repo.createPresentationRecord(bRecord)
        record <- repo.getPresentationRecord(DidCommID())
      } yield assertTrue(record.isEmpty)
    },
    test("getPresentationRecord returns all records") {
      for {
        repo <- ZIO.service[PresentationRepository]
        aRecord = presentationRecord
        bRecord = presentationRecord
        _ <- repo.createPresentationRecord(aRecord)
        _ <- repo.createPresentationRecord(bRecord)
        records <- repo.getPresentationRecords(false)
      } yield {
        assertTrue(records.size == 2) &&
        assertTrue(records.contains(aRecord)) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("getPresentationRecordByThreadId correctly returns an existing thid") {
      for {
        repo <- ZIO.service[PresentationRepository]
        thid = DidCommID()
        aRecord = presentationRecord.copy(thid = thid)
        bRecord = presentationRecord
        _ <- repo.createPresentationRecord(aRecord)
        _ <- repo.createPresentationRecord(bRecord)
        record <- repo.getPresentationRecordByThreadId(thid)
      } yield assertTrue(record.contains(aRecord))
    },
    test("getPresentationRecordByThreadId returns nothing for an unknown thid") {
      for {
        repo <- ZIO.service[PresentationRepository]
        aRecord = presentationRecord
        bRecord = presentationRecord
        _ <- repo.createPresentationRecord(aRecord)
        _ <- repo.createPresentationRecord(bRecord)
        record <- repo.getPresentationRecordByThreadId(DidCommID())
      } yield assertTrue(record.isEmpty)
    },
    test("getPresentationRecordsByStates returns valid records") {
      for {
        repo <- ZIO.service[PresentationRepository]
        aRecord = presentationRecord
        bRecord = presentationRecord
        cRecord = presentationRecord
        _ <- repo.createPresentationRecord(aRecord)
        _ <- repo.createPresentationRecord(bRecord)
        _ <- repo.createPresentationRecord(cRecord)
        _ <- repo.updatePresentationRecordProtocolState(
          aRecord.id,
          ProtocolState.RequestPending,
          ProtocolState.RequestSent
        )
        _ <- repo.updatePresentationRecordProtocolState(
          cRecord.id,
          ProtocolState.RequestPending,
          ProtocolState.PresentationReceived
        )
        pendingRecords <- repo.getPresentationRecordsByStates(
          ignoreWithZeroRetries = true,
          limit = 10,
          ProtocolState.RequestPending
        )
        otherRecords <- repo.getPresentationRecordsByStates(
          ignoreWithZeroRetries = true,
          limit = 10,
          ProtocolState.RequestSent,
          ProtocolState.PresentationReceived
        )
      } yield {
        assertTrue(pendingRecords.size == 1) &&
        assertTrue(pendingRecords.contains(bRecord)) &&
        assertTrue(otherRecords.size == 2) &&
        assertTrue(otherRecords.exists(_.id == aRecord.id)) &&
        assertTrue(otherRecords.exists(_.id == cRecord.id))
      }
    },
    test("getPresentationRecordsByStates returns an empty list if 'states' parameter is empty") {
      for {
        repo <- ZIO.service[PresentationRepository]
        aRecord = presentationRecord
        bRecord = presentationRecord
        cRecord = presentationRecord
        _ <- repo.createPresentationRecord(aRecord)
        _ <- repo.createPresentationRecord(bRecord)
        _ <- repo.createPresentationRecord(cRecord)
        records <- repo.getPresentationRecordsByStates(ignoreWithZeroRetries = true, limit = 10)
      } yield {
        assertTrue(records.isEmpty)
      }
    },
    test("updatePresentationWithCredentialsToUse updates the record") {
      for {
        repo <- ZIO.service[PresentationRepository]
        aRecord = presentationRecord
        bRecord = presentationRecord
        cRecord = presentationRecord
        _ <- repo.createPresentationRecord(aRecord)
        _ <- repo.createPresentationRecord(bRecord)
        _ <- repo.createPresentationRecord(cRecord)
        _ <- repo.updatePresentationWithCredentialsToUse(
          aRecord.id,
          Some(Seq("credential1", "credential2")),
          ProtocolState.PresentationPending
        )
        records <- repo.getPresentationRecord(aRecord.id)
      } yield {
        assertTrue(records.size == 1) &&
        assertTrue(records.exists(_.credentialsToUse.contains(Seq("credential1", "credential2"))))
      }
    },
    test("updateCredentialRecordProtocolState updates the record") {
      for {
        repo <- ZIO.service[PresentationRepository]
        aRecord = presentationRecord
        _ <- repo.createPresentationRecord(aRecord)
        record <- repo.getPresentationRecord(aRecord.id)
        count <- repo.updatePresentationRecordProtocolState(
          aRecord.id,
          ProtocolState.RequestPending,
          ProtocolState.RequestSent
        )
        updatedRecord <- repo.getPresentationRecord(aRecord.id)
      } yield {
        assertTrue(count == 1) &&
        assertTrue(record.get.protocolState == ProtocolState.RequestPending) &&
        assertTrue(updatedRecord.get.protocolState == ProtocolState.RequestSent)
      }
    },
    test("updateCredentialRecordProtocolState doesn't update the record for invalid from state") {
      for {
        repo <- ZIO.service[PresentationRepository]
        aRecord = presentationRecord
        _ <- repo.createPresentationRecord(aRecord)
        record <- repo.getPresentationRecord(aRecord.id)
        count <- repo.updatePresentationRecordProtocolState(
          aRecord.id,
          ProtocolState.PresentationPending,
          ProtocolState.RequestSent
        )
        updatedRecord <- repo.getPresentationRecord(aRecord.id)
      } yield {
        assertTrue(count == 0) &&
        assertTrue(record.get.protocolState == ProtocolState.RequestPending) &&
        assertTrue(updatedRecord.get.protocolState == ProtocolState.RequestPending)
      }
    },
    test("updateWithRequestPresentation updates record") {
      for {
        repo <- ZIO.service[PresentationRepository]
        aRecord = presentationRecord
        _ <- repo.createPresentationRecord(aRecord)
        record <- repo.getPresentationRecord(aRecord.id)
        request = requestPresentation
        count <- repo.updateWithRequestPresentation(
          aRecord.id,
          request,
          ProtocolState.RequestPending
        )
        updatedRecord <- repo.getPresentationRecord(aRecord.id)
      } yield {
        assertTrue(count == 1) &&
        assertTrue(record.get.requestPresentationData.isEmpty) &&
        assertTrue(updatedRecord.get.requestPresentationData.contains(request))
      }
    },
    test("updateWithPresentation updates record") {
      for {
        repo <- ZIO.service[PresentationRepository]
        aRecord = presentationRecord
        _ <- repo.createPresentationRecord(aRecord)
        record <- repo.getPresentationRecord(aRecord.id)
        presentation = Presentation.makePresentationFromRequest(requestPresentation.makeMessage)
        count <- repo.updateWithPresentation(
          aRecord.id,
          presentation,
          ProtocolState.PresentationPending
        )
        updatedRecord <- repo.getPresentationRecord(aRecord.id)
      } yield {
        assertTrue(count == 1) &&
        assertTrue(record.get.presentationData.isEmpty) &&
        assertTrue(updatedRecord.get.presentationData.contains(presentation))
      }
    },
    test("updateWithProposePresentation updates record") {
      for {
        repo <- ZIO.service[PresentationRepository]
        aRecord = presentationRecord
        _ <- repo.createPresentationRecord(aRecord)
        record <- repo.getPresentationRecord(aRecord.id)
        request = proposePresentation
        count <- repo.updateWithProposePresentation(
          aRecord.id,
          request,
          ProtocolState.ProposalPending
        )
        updatedRecord <- repo.getPresentationRecord(aRecord.id)
      } yield {
        assertTrue(count == 1) &&
        assertTrue(record.get.proposePresentationData.isEmpty) &&
        assertTrue(updatedRecord.get.proposePresentationData.contains(request))
      }
    },
    test("updateFail (fail one retry) updates record") {
      val aRecord = presentationRecord

      val failReason = Some("Just to test")
      for {
        repo <- ZIO.service[PresentationRepository]
        tmp <- repo.createPresentationRecord(aRecord)
        record0 <- repo.getPresentationRecord(aRecord.id)
        _ <- repo.updateAfterFail(aRecord.id, Some("Just to test")) // TEST
        updatedRecord1 <- repo.getPresentationRecord(aRecord.id)
        count <- repo.updatePresentationRecordProtocolState(
          aRecord.id,
          ProtocolState.RequestPending,
          ProtocolState.RequestSent
        )
        updatedRecord2 <- repo.getPresentationRecord(aRecord.id)
      } yield {
        assertTrue(tmp == 1) &&
        assertTrue(record0.isDefined) &&
        assertTrue(record0.get.metaRetries == maxRetries) &&
        assertTrue(updatedRecord1.get.metaRetries == (maxRetries - 1)) &&
        assertTrue(updatedRecord1.get.metaLastFailure == failReason) &&
        assertTrue(updatedRecord1.get.metaNextRetry.isDefined) &&
        // continues to work normally after retry
        assertTrue(count == 1) &&
        assertTrue(updatedRecord2.get.metaNextRetry.isDefined) &&
        assertTrue(updatedRecord2.get.metaRetries == maxRetries) &&
        assertTrue(updatedRecord2.get.metaLastFailure.isEmpty)
      }
    },
    test("updateFail (fail all retry) updates record") {
      val aRecord = presentationRecord

      for {
        repo <- ZIO.service[PresentationRepository]
        tmp <- repo.createPresentationRecord(aRecord)
        record0 <- repo.getPresentationRecord(aRecord.id)
        count1 <- repo.updateAfterFail(aRecord.id, Some("1 - Just to test"))
        count2 <- repo.updateAfterFail(aRecord.id, Some("2 - Just to test"))
        count3 <- repo.updateAfterFail(aRecord.id, Some("3 - Just to test"))
        count4 <- repo.updateAfterFail(aRecord.id, Some("4 - Just to test"))
        count5 <- repo.updateAfterFail(aRecord.id, Some("5 - Just to test"))
        count6 <- repo.updateAfterFail(aRecord.id, Some("6 - Just to test"))
        // The 6 retry should not happen since the max retries is 5
        // (but should also not have an effect other that update the error message)
        updatedRecord1 <- repo.getPresentationRecord(aRecord.id)
      } yield {

        assertTrue(tmp == 1) &&
        assertTrue(count1 == 1) &&
        assertTrue(count2 == 1) &&
        assertTrue(count3 == 1) &&
        assertTrue(count4 == 1) &&
        assertTrue(count5 == 1) &&
        assertTrue(record0.isDefined) &&
        assertTrue(record0.get.metaRetries == maxRetries) &&
        assertTrue(updatedRecord1.get.metaRetries == 0) && // assume the max retries is 5
        assertTrue(updatedRecord1.get.metaNextRetry.isEmpty) &&
        assertTrue(updatedRecord1.get.metaLastFailure.contains("6 - Just to test"))

      }
    }
  ).provideSomeLayer(ZLayer.succeed(WalletAccessContext(WalletId.random)))

  val multitenantTestSuite = suite("muilti-tenant CRUD operation")(
    test("do not see PresentationRecord outside of the wallet") {
      val walletId1 = WalletId.random
      val walletId2 = WalletId.random
      val wallet1 = ZLayer.succeed(WalletAccessContext(walletId1))
      val wallet2 = ZLayer.succeed(WalletAccessContext(walletId2))
      for {
        repo <- ZIO.service[PresentationRepository]
        record1 = presentationRecord
        record2 = presentationRecord
        count1 <- repo.createPresentationRecord(record1).provide(wallet1)
        count2 <- repo.createPresentationRecord(record2).provide(wallet2)
        ownWalletRecords1 <- repo.getPresentationRecords(false).provide(wallet1)
        ownWalletRecords2 <- repo.getPresentationRecords(false).provide(wallet2)
        crossWalletRecordById <- repo.getPresentationRecord(record2.id).provide(wallet1)
        crossWalletRecordByThid <- repo.getPresentationRecordByThreadId(record2.thid).provide(wallet1)
      } yield assert(count1)(equalTo(1)) &&
        assert(count2)(equalTo(1)) &&
        assert(ownWalletRecords1)(hasSameElements(Seq(record1))) &&
        assert(ownWalletRecords2)(hasSameElements(Seq(record2))) &&
        assert(crossWalletRecordById)(isNone) &&
        assert(crossWalletRecordByThid)(isNone)
    },
    test("unable to update PresentationRecord outside of the wallet") {
      val walletId1 = WalletId.random
      val walletId2 = WalletId.random
      val wallet1 = ZLayer.succeed(WalletAccessContext(walletId1))
      val wallet2 = ZLayer.succeed(WalletAccessContext(walletId2))
      val newState = PresentationRecord.ProtocolState.PresentationVerified
      for {
        repo <- ZIO.service[PresentationRepository]
        record1 = presentationRecord
        record2 = presentationRecord
        count1 <- repo.createPresentationRecord(record1).provide(wallet1)
        update1 <- repo.updatePresentationWithCredentialsToUse(record2.id, Option(Nil), newState).provide(wallet2)
        update2 <- repo.updateAfterFail(record2.id, Some("fail reason")).provide(wallet2)
        update3 <- repo
          .updatePresentationRecordProtocolState(record2.id, record1.protocolState, newState)
          .provide(wallet2)
      } yield assert(count1)(equalTo(1)) &&
        assert(update1)(isZero) &&
        assert(update2)(isZero) &&
        assert(update3)(isZero)
    }
  )
}
