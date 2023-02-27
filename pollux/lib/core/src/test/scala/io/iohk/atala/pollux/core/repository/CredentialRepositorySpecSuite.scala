package io.iohk.atala.pollux.core.repository

import com.squareup.okhttp.Protocol
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.issuecredential.IssueCredential
import io.iohk.atala.mercury.protocol.issuecredential.RequestCredential
import io.iohk.atala.pollux.core.model._
import io.iohk.atala.pollux.core.model.IssueCredentialRecord._
import io.iohk.atala.pollux.core.model.error.CredentialRepositoryError._
import io.iohk.atala.prism.identity.Did
import zio.Cause
import zio.Exit
import zio.Task
import zio.ZIO
import zio.test.Assertion._
import zio.test._

import java.time.Instant
import java.util.UUID
import io.iohk.atala.castor.core.model.did.PrismDID

object CredentialRepositorySpecSuite {
  val maxRetries = 5 // TODO Move to config

  private def issueCredentialRecord = IssueCredentialRecord(
    id = DidCommID(),
    createdAt = Instant.ofEpochSecond(Instant.now.getEpochSecond()),
    updatedAt = None,
    thid = DidCommID(),
    schemaId = None,
    role = IssueCredentialRecord.Role.Issuer,
    subjectId = "did:prism:HOLDER",
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
        repo <- ZIO.service[CredentialRepository[Task]]
        record = issueCredentialRecord
        count <- repo.createIssueCredentialRecord(record)
      } yield assertTrue(count == 1)
    },
    test("createIssueCredentialRecord prevents creation of 2 records with the same thid") {
      for {
        repo <- ZIO.service[CredentialRepository[Task]]
        thid = DidCommID()
        aRecord = issueCredentialRecord.copy(thid = thid)
        bRecord = issueCredentialRecord.copy(thid = thid)
        aCount <- repo.createIssueCredentialRecord(aRecord)
        bCount <- repo.createIssueCredentialRecord(bRecord).exit
      } yield assertTrue(aCount == 1) && assert(bCount)(fails(isSubtype[UniqueConstraintViolation](anything)))
    },
    test("createIssueCredentialRecord correctly read and write on non-null issuingDID") {
      for {
        repo <- ZIO.service[CredentialRepository[Task]]
        issuingDID <- ZIO.fromEither(PrismDID.buildCanonicalFromSuffix("0" * 64))
        record = issueCredentialRecord.copy(issuingDID = Some(issuingDID))
        count <- repo.createIssueCredentialRecord(record)
        readRecord <- repo.getIssueCredentialRecord(record.id)
      } yield assertTrue(count == 1) && assert(readRecord)(isSome(equalTo(record)))
    },
    test("getIssueCredentialRecord correctly returns an existing record") {
      for {
        repo <- ZIO.service[CredentialRepository[Task]]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        record <- repo.getIssueCredentialRecord(bRecord.id)
      } yield assertTrue(record.contains(bRecord))
    },
    test("getIssuanceCredentialRecord returns None for an unknown record") {
      for {
        repo <- ZIO.service[CredentialRepository[Task]]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        record <- repo.getIssueCredentialRecord(DidCommID())
      } yield assertTrue(record.isEmpty)
    },
    test("getIssuanceCredentialRecord returns all records") {
      for {
        repo <- ZIO.service[CredentialRepository[Task]]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        records <- repo.getIssueCredentialRecords()
      } yield {
        assertTrue(records.size == 2) &&
        assertTrue(records.contains(aRecord)) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("deleteIssueCredentialRecord deletes an exsiting record") {
      for {
        repo <- ZIO.service[CredentialRepository[Task]]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        count <- repo.deleteIssueCredentialRecord(aRecord.id)
        records <- repo.getIssueCredentialRecords()
      } yield {
        assertTrue(count == 1) &&
        assertTrue(records.size == 1) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("deleteIssueCredentialRecord does nothing for an unknown record") {
      for {
        repo <- ZIO.service[CredentialRepository[Task]]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        count <- repo.deleteIssueCredentialRecord(DidCommID())
        records <- repo.getIssueCredentialRecords()
      } yield {
        assertTrue(count == 0) &&
        assertTrue(records.size == 2) &&
        assertTrue(records.contains(aRecord)) &&
        assertTrue(records.contains(bRecord))
      }
    },
    test("getIssueCredentialRecordByThreadId correctly returns an existing thid") {
      for {
        repo <- ZIO.service[CredentialRepository[Task]]
        thid = DidCommID()
        aRecord = issueCredentialRecord.copy(thid = thid)
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        record <- repo.getIssueCredentialRecordByThreadId(thid)
      } yield assertTrue(record.contains(aRecord))
    },
    test("getIssueCredentialRecordByThreadId returns nothing for an unknown thid") {
      for {
        repo <- ZIO.service[CredentialRepository[Task]]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        record <- repo.getIssueCredentialRecordByThreadId(DidCommID())
      } yield assertTrue(record.isEmpty)
    },
    test("getIssueCredentialRecordsByStates returns valid records") {
      for {
        repo <- ZIO.service[CredentialRepository[Task]]
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
        pendingRecords <- repo.getIssueCredentialRecordsByStates(ProtocolState.OfferPending)
        otherRecords <- repo.getIssueCredentialRecordsByStates(
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
        repo <- ZIO.service[CredentialRepository[Task]]
        aRecord = issueCredentialRecord
        bRecord = issueCredentialRecord
        cRecord = issueCredentialRecord
        _ <- repo.createIssueCredentialRecord(aRecord)
        _ <- repo.createIssueCredentialRecord(bRecord)
        _ <- repo.createIssueCredentialRecord(cRecord)
        records <- repo.getIssueCredentialRecordsByStates()
      } yield {
        assertTrue(records.isEmpty)
      }
    },
    test("getValidIssuedCredentials returns valid records") {
      for {
        repo <- ZIO.service[CredentialRepository[Task]]
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
        repo <- ZIO.service[CredentialRepository[Task]]
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
        repo <- ZIO.service[CredentialRepository[Task]]
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
        repo <- ZIO.service[CredentialRepository[Task]]
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
        repo <- ZIO.service[CredentialRepository[Task]]
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
        repo <- ZIO.service[CredentialRepository[Task]]
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
        repo <- ZIO.service[CredentialRepository[Task]]
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
        repo <- ZIO.service[CredentialRepository[Task]]
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
        repo <- ZIO.service[CredentialRepository[Task]]
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
        repo <- ZIO.service[CredentialRepository[Task]]
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
  )
}
