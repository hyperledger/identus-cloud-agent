package io.iohk.atala.agent.server.jobs

import scala.jdk.CollectionConverters.*

import zio.*
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.pollux.core.model.IssueCredentialRecord

import io.iohk.atala.mercury.DidComm
import io.iohk.atala.mercury.model._
import io.iohk.atala.mercury.model.error._
import io.iohk.atala.mercury.protocol.issuecredential._
import io.iohk.atala.mercury.AgentCli
import java.io.IOException
import io.iohk.atala.resolvers.UniversalDidResolver
import zhttp.service._
import zhttp.http._
import io.iohk.atala.mercury.MediaTypes
import io.iohk.atala.mercury.AgentCli._

object BackgroundJobs {

  val didCommExchanges = {
    for {
      credentialService <- ZIO.service[CredentialService]
      records <- credentialService
        .getCredentialRecords()
        .mapError(err => Throwable(s"Error occured while getting issue credential records: $err"))
      _ <- ZIO.foreach(records)(performExchange)
    } yield ()
  }

  private[this] def performExchange(
      record: IssueCredentialRecord
  ): ZIO[DidComm & CredentialService, Throwable, Unit] = {
    import IssueCredentialRecord.State._
    val aux = for {
      _ <- Console.printLine(s"Running action with record => $record")

      _ <- record match {
        case IssueCredentialRecord(id, _, _, subjectId, _, claims, OfferPending, _, _, _) =>
          val attributes = claims.map { case (k, v) => Attribute(k, v) }
          val credentialPreview = CredentialPreview(attributes = attributes.toSeq)
          val body = OfferCredential.Body(goal_code = Some("Offer Credential"), credential_preview = credentialPreview)
          val attachmentDescriptor =
            AttachmentDescriptor.buildAttachment[CredentialPreview](payload = credentialPreview)

          for {
            _ <- ZIO.log(s"IssueCredentialRecord: OfferPending (START)")
            didComm <- ZIO.service[DidComm]
            offer = OfferCredential( // TODO
              body = body,
              attachments = Seq(attachmentDescriptor),
              to = DidId(subjectId),
              from = didComm.myDid
            )
            msg = offer.makeMessage

            _ <- AgentCli.sendMessage(msg)
            credentialService <- ZIO.service[CredentialService]
            _ <- credentialService.markRequestSent(id)
            _ <- ZIO.log(s"IssueCredentialRecord: OfferPending (END)")
          } yield ()

        case IssueCredentialRecord(id, _, _, subjectId, _, _, RequestPending, Some(offerData), _, _) =>
          for {
            didCommService <- ZIO.service[DidComm]
            requestCredential = RequestCredential(
              body = RequestCredential.Body(
                goal_code = offerData.body.goal_code,
                comment = offerData.body.comment,
                formats = offerData.body.formats
              ),
              attachments = offerData.attachments,
              thid = offerData.thid,
              from = offerData.to,
              to = offerData.from
            )
            msg = requestCredential.makeMessage
            _ <- sendMessage(msg)
            credentialService <- ZIO.service[CredentialService]
            _ <- credentialService.markRequestSent(id)
            _ <- ZIO.log(s"IssueCredentialRecord: RequestPending id='$id' (END)")
          } yield ()

        case IssueCredentialRecord(_, _, _, subjectId, _, _, ProblemReportPending, _, _, _) => ???
        case IssueCredentialRecord(id, _, _, subjectId, _, _, CredentialPending, _, Some(requestCredentialData), _) =>
          val issueCredential = IssueCredential(
            body = IssueCredential.Body(
              goal_code = requestCredentialData.body.goal_code,
              comment = requestCredentialData.body.comment,
              replacement_id = None,
              more_available = None,
              formats = requestCredentialData.body.formats
            ),
            attachments = requestCredentialData.attachments,
            thid = requestCredentialData.thid,
            from = requestCredentialData.to,
            to = requestCredentialData.from
          )

          for {
            didCommService <- ZIO.service[DidComm]
            _ <- sendMessage(issueCredential.makeMessage)
            credentialService <- ZIO.service[CredentialService]
            _ <- credentialService.markCredentialSent(id)
            _ <- ZIO.log(s"IssueCredentialRecord: RequestPending id='$id' (END)")
          } yield ()
        case IssueCredentialRecord(_, _, _, _, _, _, _, _, _, _) => ZIO.unit
      }
    } yield ()

    aux.catchAll {
      case ex: TransportError => // : io.iohk.atala.mercury.model.error.MercuryError | java.io.IOException =>
        ex.printStackTrace()
        ZIO.logError(ex.getMessage()) *>
          ZIO.fail(mercuryErrorAsThrowable(ex))
      case ex: IOException => ZIO.fail(ex)
    }
  }

}
