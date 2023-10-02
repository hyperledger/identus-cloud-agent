package io.iohk.atala.pollux.core.repository

import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.issuecredential.{IssueCredential, RequestCredential}
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.*
import io.iohk.atala.pollux.core.model.error.CredentialRepositoryError.*
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import zio.test.*
import zio.test.Assertion.*
import zio.{Exit, ZIO, ZLayer}

import java.time.Instant
import java.util.UUID

object CredentialRepositorySpecSuite {
  val maxRetries = 5 // TODO Move to config

  private def issueCredentialRecord = IssueCredentialRecord(
    id = DidCommID(),
    createdAt = Instant.now,
    updatedAt = None,
    thid = DidCommID(),
    schemaId = None,
    role = IssueCredentialRecord.Role.Issuer,
    subjectId = None,
    validityPeriod = None,
    automaticIssuance = None,
    awaitConfirmation = None,
    protocolState = IssueCredentialRecord.ProtocolState.OfferPending,
    publicationState = None,
    offerCredentialData = None,
    requestCredentialData = None,
    issueCredentialData = None,
    issuedCredentialRaw = None,
    issuingDID = None,
    metaRetries = maxRetries,
    metaNextRetry = Some(Instant.now()),
    metaLastFailure = None,
  )

  private def requestCredential = RequestCredential(
    from = DidId("did:prism:aaa"),
    to = DidId("did:prism:bbb"),
    thid = Some(UUID.randomUUID.toString),
    body = RequestCredential.Body(goal_code = Some("credential issuance")),
    attachments = Nil
  )

  val testSuite = suite("CRUD operations")(
    test("createIssueCredentialRecord creates a new record in DB") {
      for {
        repo <- ZIO.service[CredentialRepository]
        record = issueCredentialRecord
        count <- repo.createIssueCredentialRecord(record)
      } yield assertTrue(count == 1)
    },
    test("createIssueCredentialRecord prevents creation of 2 records with the same thid") {
      for {
        repo <- ZIO.service[CredentialRepository]
        thid = DidCommID()
        aRecord = issueCredentialRecord.copy(thid = thid)
        bRecord = issueCredentialRecord.copy(thid = thid)
        aCount <- repo.createIssueCredentialRecord(aRecord)
        bCount <- repo.createIssueCredentialRecord(bRecord).exit
      } yield assertTrue(aCount == 1) && assert(bCount)(fails(isSubtype[UniqueConstraintViolation](anything)))
    },
    test("createIssueCredentialRecord correctly read and write on non-null issuingDID") {
      for {
        repo <- ZIO.service[CredentialRepository]
        issuingDID <- ZIO.fromEither(PrismDID.buildCanonicalFromSuffix("0" * 64))
        record = issueCredentialRecord.copy(issuingDID = Some(issuingDID))
        count <- repo.createIssueCredentialRecord(record)
        readRecord <- repo.getIssueCredentialRecord(record.id)
      } yield assertTrue(count == 1) && assert(readRecord)(isSome(equalTo(record)))
    },
    test("getIssueCredentialRecord correctly returns an existing record") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        record <- repo.getIssueCredentialRecord(bRecord.id)
      } yield assertTrue(record.contains(bRecord))
    },
    test("getIssuanceCredentialRecord returns None for an unknown record") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        record <- repo.getIssueCredentialRecord(DidCommID())
      } yield assertTrue(record.isEmpty)
    },
    test("getIssuanceCredentialRecord returns all records") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        records <- repo.getIssueCredentialRecords(false).map(_._1)
      } yield {
        assertTrue(records.size == 2) &&
        assertTrue(records.contains(aRecord)) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("getIssuanceCredentialRecord returns records with offset") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        records <- repo.getIssueCredentialRecords(false, offset = Some(1)).map(_._1)
      } yield {
        assertTrue(records.size == 1) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("getIssuanceCredentialRecord returns records with limit") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        records <- repo.getIssueCredentialRecords(false, limit = Some(1)).map(_._1)
      } yield {
        assertTrue(records.size == 1) &&
        assertTrue(records.contains(aRecord))
      }
    },
    test("getIssuanceCredentialRecord returns records with offset and limit") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        cRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        _ <- repo.createIssueCredentialRecord(cRecord)
        records <- repo
          .getIssueCredentialRecords(false, offset = Some(1), limit = Some(1))
          .map(_._1)
      } yield {
        assertTrue(records.size == 1) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("deleteIssueCredentialRecord deletes an exsiting record") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        count <- repo.deleteIssueCredentialRecord(aRecord.id)
        records <- repo.getIssueCredentialRecords(false).map(_._1)
      } yield {
        assertTrue(count == 1) &&
        assertTrue(records.size == 1) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("deleteIssueCredentialRecord does nothing for an unknown record") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        count <- repo.deleteIssueCredentialRecord(DidCommID())
        records <- repo.getIssueCredentialRecords(false).map(_._1)
      } yield {
        assertTrue(count == 0) &&
        assertTrue(records.size == 2) &&
        assertTrue(records.contains(aRecord)) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("getIssueCredentialRecordByThreadId correctly returns an existing thid") {
      for {
        repo <- ZIO.service[CredentialRepository]
        thid = DidCommID()
        aRecord = issueCredentialRecord.copy(thid = thid)
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        record <- repo.getIssueCredentialRecordByThreadId(thid, false)
      } yield assertTrue(record.contains(aRecord))
    },
    test("getIssueCredentialRecordByThreadId returns nothing for an unknown thid") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        record <- repo.getIssueCredentialRecordByThreadId(DidCommID(), false)
      } yield assertTrue(record.isEmpty)
    },
    test("getIssueCredentialRecordsByStates returns valid records") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        cRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        _ <- repo.createIssueCredentialRecord(cRecord)
        _ <- repo.updateCredentialRecordProtocolState(aRecord.id, ProtocolState.OfferPending, ProtocolState.OfferSent)
        _ <- repo.updateCredentialRecordProtocolState(
          cRecord.id,
          ProtocolState.OfferPending,
          ProtocolState.CredentialGenerated
        )
        pendingRecords <- repo.getIssueCredentialRecordsByStates(
          ignoreWithZeroRetries = true,
          limit = 10,
          ProtocolState.OfferPending
        )
        otherRecords <- repo.getIssueCredentialRecordsByStates(
          ignoreWithZeroRetries = true,
          limit = 10,
          ProtocolState.OfferSent,
          ProtocolState.CredentialGenerated
        )
      } yield {
        assertTrue(pendingRecords.size == 1) &&
        assertTrue(pendingRecords.contains(bRecord)) &&
        assertTrue(otherRecords.size == 2) &&
        assertTrue(otherRecords.exists(_.id == aRecord.id)) &&
        assertTrue(otherRecords.exists(_.id == cRecord.id))
      }
    },
    test("getIssueCredentialRecordsByStates returns an empty list if 'states' parameter is empty") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        cRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        _ <- repo.createIssueCredentialRecord(cRecord)
        records <- repo.getIssueCredentialRecordsByStates(ignoreWithZeroRetries = true, limit = 10)
      } yield {
        assertTrue(records.isEmpty)
      }
    },
    test("getValidIssuedCredentials returns valid records") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        cRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        _ <- repo.createIssueCredentialRecord(cRecord)
        _ <- repo.updateWithIssuedRawCredential(
          aRecord.id,
          IssueCredential.makeIssueCredentialFromRequestCredential(requestCredential.makeMessage),
          "RAW_CREDENTIAL_DATA",
          ProtocolState.CredentialReceived
        )
        records <- repo.getValidIssuedCredentials(Seq(aRecord.id, bRecord.id))
      } yield {
        assertTrue(records.size == 1) &&
        assertTrue(
          records.contains(ValidIssuedCredentialRecord(aRecord.id, Some("RAW_CREDENTIAL_DATA"), aRecord.subjectId))
        )
      }
    },
    test("updateCredentialRecordProtocolState updates the record") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        record <- repo.getIssueCredentialRecord(aRecord.id)
        count <- repo.updateCredentialRecordProtocolState(
          aRecord.id,
          ProtocolState.OfferPending,
          ProtocolState.OfferSent
        )
        updatedRecord <- repo.getIssueCredentialRecord(aRecord.id)
      } yield {
        assertTrue(count == 1) &&
        assertTrue(record.get.protocolState == ProtocolState.OfferPending) &&
        assertTrue(updatedRecord.get.protocolState == ProtocolState.OfferSent)
      }
    },
    test("updateCredentialRecordProtocolState doesn't update the record for invalid from state") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        record <- repo.getIssueCredentialRecord(aRecord.id)
        count <- repo.updateCredentialRecordProtocolState(
          aRecord.id,
          ProtocolState.RequestPending,
          ProtocolState.RequestSent
        )
        updatedRecord <- repo.getIssueCredentialRecord(aRecord.id)
      } yield {
        assertTrue(count == 0) &&
        assertTrue(record.get.protocolState == ProtocolState.OfferPending) &&
        assertTrue(updatedRecord.get.protocolState == ProtocolState.OfferPending)
      }
    },
    test("updateCredentialRecordPublicationState updates the record") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        record <- repo.getIssueCredentialRecord(aRecord.id)
        count <- repo.updateCredentialRecordPublicationState(
          aRecord.id,
          None,
          Some(PublicationState.PublicationPending)
        )
        updatedRecord <- repo.getIssueCredentialRecord(aRecord.id)
      } yield {
        assertTrue(count == 1) &&
        assertTrue(record.get.publicationState.isEmpty) &&
        assertTrue(updatedRecord.get.publicationState.contains(PublicationState.PublicationPending))
      }
    },
    test("updateCredentialRecordPublicationState doesn't update the record for invalid from state") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        record <- repo.getIssueCredentialRecord(aRecord.id)
        count <- repo.updateCredentialRecordPublicationState(
          aRecord.id,
          Some(PublicationState.PublicationPending),
          Some(PublicationState.PublicationQueued)
        )
        updatedRecord <- repo.getIssueCredentialRecord(aRecord.id)
      } yield {
        assertTrue(count == 0) &&
        assertTrue(record.get.publicationState.isEmpty) &&
        assertTrue(updatedRecord.get.publicationState.isEmpty)
      }
    },
    test("updateWithRequestCredential updates record") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        record <- repo.getIssueCredentialRecord(aRecord.id)
        request = requestCredential
        count <- repo.updateWithRequestCredential(
          aRecord.id,
          request,
          ProtocolState.RequestPending
        )
        updatedRecord <- repo.getIssueCredentialRecord(aRecord.id)
      } yield {
        assertTrue(count == 1) &&
        assertTrue(record.get.requestCredentialData.isEmpty) &&
        assertTrue(updatedRecord.get.requestCredentialData.contains(request))
      }
    },
    test("updateWithIssueCredential updates record") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        record <- repo.getIssueCredentialRecord(aRecord.id)
        issueCredential = IssueCredential.makeIssueCredentialFromRequestCredential(requestCredential.makeMessage)
        count <- repo.updateWithIssueCredential(
          aRecord.id,
          issueCredential,
          ProtocolState.CredentialPending
        )
        updatedRecord <- repo.getIssueCredentialRecord(aRecord.id)
      } yield {
        assertTrue(count == 1) &&
        assertTrue(record.get.issueCredentialData.isEmpty) &&
        assertTrue(updatedRecord.get.issueCredentialData.contains(issueCredential))
      }
    },
    test("updateWithIssuedRawCredential updates record") {
      for {
        repo <- ZIO.service[CredentialRepository]
        aRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        record <- repo.getIssueCredentialRecord(aRecord.id)
        issueCredential = IssueCredential.makeIssueCredentialFromRequestCredential(requestCredential.makeMessage)
        count <- repo.updateWithIssuedRawCredential(
          aRecord.id,
          issueCredential,
          "RAW_CREDENTIAL_DATA",
          ProtocolState.CredentialReceived
        )
        updatedRecord <- repo.getIssueCredentialRecord(aRecord.id)
      } yield {
        assertTrue(count == 1) &&
        assertTrue(record.get.issueCredentialData.isEmpty) &&
        assertTrue(updatedRecord.get.issueCredentialData.contains(issueCredential)) &&
        assertTrue(updatedRecord.get.issuedCredentialRaw.contains("RAW_CREDENTIAL_DATA"))
      }
    },
    test("updateFail (fail one retry) updates record") {
      val aRecord = issueCredentialRecord

      val failReason = Some("Just to test")
      for {
        repo <- ZIO.service[CredentialRepository]
        tmp <- repo.createIssueCredentialRecord(aRecord)
        record0 <- repo.getIssueCredentialRecord(aRecord.id)
        _ <- repo.updateAfterFail(aRecord.id, Some("Just to test")) // TEST
        updatedRecord1 <- repo.getIssueCredentialRecord(aRecord.id)
        count <- repo.updateCredentialRecordProtocolState(
          aRecord.id,
          ProtocolState.OfferPending,
          ProtocolState.OfferSent
        )
        updatedRecord2 <- repo.getIssueCredentialRecord(aRecord.id)
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
        assertTrue(updatedRecord2.get.metaLastFailure == None)
      }
    },
    test("updateFail (fail all retry) updates record") {
      val aRecord = issueCredentialRecord

      for {
        repo <- ZIO.service[CredentialRepository]
        tmp <- repo.createIssueCredentialRecord(aRecord)
        record0 <- repo.getIssueCredentialRecord(aRecord.id)
        count1 <- repo.updateAfterFail(aRecord.id, Some("1 - Just to test"))
        count2 <- repo.updateAfterFail(aRecord.id, Some("2 - Just to test"))
        count3 <- repo.updateAfterFail(aRecord.id, Some("3 - Just to test"))
        count4 <- repo.updateAfterFail(aRecord.id, Some("4 - Just to test"))
        count5 <- repo.updateAfterFail(aRecord.id, Some("5 - Just to test"))
        count6 <- repo.updateAfterFail(aRecord.id, Some("6 - Just to test"))
        // The 6 retry should not happen since the max retries is 5
        // (but should also not have an effect other that update the error message)
        updatedRecord1 <- repo.getIssueCredentialRecord(aRecord.id)
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
        assertTrue(updatedRecord1.get.metaLastFailure == Some("6 - Just to test"))

      }
    }
  ).provideSomeLayer(ZLayer.succeed(WalletAccessContext(WalletId.random)))

  val multitenantTestSuite = suite("multi-tenant CRUD operations")(
    test("do not see IssueCredentialRecord outside of the wallet") {
      val walletId1 = WalletId.random
      val walletId2 = WalletId.random
      val wallet1 = ZLayer.succeed(WalletAccessContext(walletId1))
      val wallet2 = ZLayer.succeed(WalletAccessContext(walletId2))
      for {
        repo <- ZIO.service[CredentialRepository]
        record1 = issueCredentialRecord
        record2 = issueCredentialRecord
        count1 <- repo.createIssueCredentialRecord(record1).provide(wallet1)
        count2 <- repo.createIssueCredentialRecord(record2).provide(wallet2)
        ownWalletRecords1 <- repo.getIssueCredentialRecords(false).provide(wallet1)
        ownWalletRecords2 <- repo.getIssueCredentialRecords(false).provide(wallet2)
        crossWalletRecordById <- repo.getIssueCredentialRecord(record2.id).provide(wallet1)
        crossWalletRecordByThid <- repo.getIssueCredentialRecordByThreadId(record2.thid, false).provide(wallet1)
      } yield assert(count1)(equalTo(1)) &&
        assert(count2)(equalTo(1)) &&
        assert(ownWalletRecords1._1)(hasSameElements(Seq(record1))) &&
        assert(ownWalletRecords2._1)(hasSameElements(Seq(record2))) &&
        assert(crossWalletRecordById)(isNone) &&
        assert(crossWalletRecordByThid)(isNone)
    },
    test("unable to update IssueCredentialRecord outside of the wallet") {
      val walletId1 = WalletId.random
      val walletId2 = WalletId.random
      val wallet1 = ZLayer.succeed(WalletAccessContext(walletId1))
      val wallet2 = ZLayer.succeed(WalletAccessContext(walletId2))
      val newState = IssueCredentialRecord.ProtocolState.OfferReceived
      for {
        repo <- ZIO.service[CredentialRepository]
        record1 = issueCredentialRecord
        record2 = issueCredentialRecord
        count1 <- repo.createIssueCredentialRecord(record1).provide(wallet1)
        update1 <- repo.updateWithSubjectId(record2.id, "my-id", newState).provide(wallet2)
        update2 <- repo.updateAfterFail(record2.id, Some("fail reason")).provide(wallet2)
        update3 <- repo
          .updateCredentialRecordProtocolState(record2.id, record1.protocolState, newState)
          .provide(wallet2)
      } yield assert(count1)(equalTo(1)) &&
        assert(update1)(isZero) &&
        assert(update2)(isZero) &&
        assert(update3)(isZero)
    },
    test("unable to delete IssueCredentialRecord outside of the wallet") {
      val walletId1 = WalletId.random
      val walletId2 = WalletId.random
      val wallet1 = ZLayer.succeed(WalletAccessContext(walletId1))
      val wallet2 = ZLayer.succeed(WalletAccessContext(walletId2))
      for {
        repo <- ZIO.service[CredentialRepository]
        record1 = issueCredentialRecord
        count1 <- repo.createIssueCredentialRecord(record1).provide(wallet1)
        delete1 <- repo.deleteIssueCredentialRecord(record1.id).provide(wallet2)
      } yield assert(count1)(equalTo(1)) && assert(delete1)(isZero)
    }
  )
}
