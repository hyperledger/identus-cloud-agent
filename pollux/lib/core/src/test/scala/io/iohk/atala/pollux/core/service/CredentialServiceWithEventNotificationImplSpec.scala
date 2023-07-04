package io.iohk.atala.pollux.core.service

import io.circe.syntax.*
import io.iohk.atala.event.notification.{EventNotificationService, EventNotificationServiceImpl}
import io.iohk.atala.mercury.model.Message
import io.iohk.atala.mercury.protocol.issuecredential.*
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.model.IssueCredentialRecord.ProtocolState
import io.iohk.atala.pollux.core.model.error.CredentialServiceError
import io.iohk.atala.pollux.core.repository.CredentialRepositoryInMemory
import io.iohk.atala.pollux.vc.jwt.*
import zio.*
import zio.test.*

object CredentialServiceWithEventNotificationImplSpec extends ZIOSpecDefault with CredentialServiceSpecHelper {

  private val eventNotificationService = ZLayer.succeed(50) >>> EventNotificationServiceImpl.layer
  private val credentialServiceWithEventNotificationLayer =
    irisStubLayer ++ CredentialRepositoryInMemory.layer ++ didResolverLayer ++ ResourceURIDereferencerImpl.layer
      >>> CredentialServiceWithEventNotificationImpl.layer

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("CredentialServiceWithEventNotificationImpl") {
      test("Happy flow generates relevant events") {
        for {
          // Get issuer services
          issuerServices <- (for {
            issuerSvc <- ZIO.service[CredentialService]
            issuerEns <- ZIO.service[EventNotificationService]
          } yield (issuerSvc, issuerEns))
            .provide(eventNotificationService, credentialServiceWithEventNotificationLayer)
          issuerSvc = issuerServices._1
          issuerEns = issuerServices._2

          // Get Holder services
          holderServices <- (for {
            holderSvc <- ZIO.service[CredentialService]
            holderEns <- ZIO.service[EventNotificationService]
          } yield (holderSvc, holderEns))
            .provide(eventNotificationService, credentialServiceWithEventNotificationLayer)
          holderSvc = holderServices._1
          holderEns = holderServices._2

          // Issuer creates offer
          offerCreatedRecord <- issuerSvc.createRecord()
          issuerRecordId = offerCreatedRecord.id
          // Issuer sends offer
          _ <- issuerSvc.markOfferSent(issuerRecordId)
          msg <- ZIO.fromEither(offerCreatedRecord.offerCredentialData.get.makeMessage.asJson.as[Message])
          // Holder receives offer
          offerReceivedRecord <- holderSvc.receiveCredentialOffer(OfferCredential.readFromMessage(msg))
          holderRecordId = offerReceivedRecord.id
          subjectId = "did:prism:60821d6833158c93fde5bb6a40d69996a683bf1fa5cdf32c458395b2887597c3"
          // Holder accepts offer
          _ <- holderSvc.acceptCredentialOffer(holderRecordId, subjectId)
          // Holder generates proof
          requestGeneratedRecord <- holderSvc.generateCredentialRequest(offerReceivedRecord.id, JWT("Fake JWT"))
          // Holder sends offer
          _ <- holderSvc.markRequestSent(holderRecordId)
          msg <- ZIO.fromEither(requestGeneratedRecord.requestCredentialData.get.makeMessage.asJson.as[Message])
          // Issuer receives request
          requestReceivedRecord <- issuerSvc.receiveCredentialRequest(RequestCredential.readFromMessage(msg))
          // Issuer accepts request
          requestAcceptedRecord <- issuerSvc.acceptCredentialRequest(issuerRecordId)
          // Issuer generates credential
          issue = issueCredential(Some(requestAcceptedRecord.thid))
          credentialGenerateRecord <- issuerSvc.markCredentialGenerated(issuerRecordId, issue)
          // Issuer sends credential
          _ <- issuerSvc.markCredentialSent(issuerRecordId)
          msg <- ZIO.fromEither(credentialGenerateRecord.issueCredentialData.get.makeMessage.asJson.as[Message])
          // Holder receives credential
          _ <- holderSvc.receiveCredentialIssue(IssueCredential.readFromMessage(msg))
          // Get generated events
          issuerConsumer <- issuerEns.consumer[IssueCredentialRecord]("Issue")
          issuerEvents <- issuerConsumer.poll(50)
          holderConsumer <- holderEns.consumer[IssueCredentialRecord]("Issue")
          holderEvents <- holderConsumer.poll(50)
        } yield {
          assertTrue(issuerEvents.size == 6) &&
          assertTrue(issuerEvents.head.data.protocolState == ProtocolState.OfferPending) &&
          assertTrue(issuerEvents(1).data.protocolState == ProtocolState.OfferSent) &&
          assertTrue(issuerEvents(2).data.protocolState == ProtocolState.RequestReceived) &&
          assertTrue(issuerEvents(3).data.protocolState == ProtocolState.CredentialPending) &&
          assertTrue(issuerEvents(4).data.protocolState == ProtocolState.CredentialGenerated) &&
          assertTrue(issuerEvents(5).data.protocolState == ProtocolState.CredentialSent) &&
          assertTrue(holderEvents.size == 5) &&
          assertTrue(holderEvents.head.data.protocolState == ProtocolState.OfferReceived) &&
          assertTrue(holderEvents(1).data.protocolState == ProtocolState.RequestPending) &&
          assertTrue(holderEvents(2).data.protocolState == ProtocolState.RequestGenerated) &&
          assertTrue(holderEvents(3).data.protocolState == ProtocolState.RequestSent) &&
          assertTrue(holderEvents(4).data.protocolState == ProtocolState.CredentialReceived)
        }
      }
    }
  }

}
