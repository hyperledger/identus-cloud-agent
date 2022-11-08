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

  private[this] def performExchange(record: IssueCredentialRecord): ZIO[DidComm, Throwable, Unit] = {
    import IssueCredentialRecord.State._
    val aux = for {
      _ <- Console.printLine(s"Running action with record => $record")

      _ <- record match {
        case IssueCredentialRecord(_, _, _, subjectId, _, claims, OfferPending) =>
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
            _ <- ZIO.log(s"IssueCredentialRecord: OfferPending (END)")
            // TODO UPDATE STATE
          } yield ()

        case IssueCredentialRecord(_, _, _, subjectId, _, _, RequestPending)       => ???
        case IssueCredentialRecord(_, _, _, subjectId, _, _, ProblemReportPending) => ???
        case IssueCredentialRecord(_, _, _, subjectId, _, _, CredentialPending)    => ???
        case IssueCredentialRecord(_, _, _, _, _, _, _)                            => ZIO.unit
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
