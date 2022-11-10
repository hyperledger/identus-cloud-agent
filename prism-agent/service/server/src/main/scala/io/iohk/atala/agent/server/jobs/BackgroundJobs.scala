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
        .getIssueCredentialRecords()
        .mapError(err => Throwable(s"Error occured while getting issue credential records: $err"))
      _ <- ZIO.foreach(records)(performExchange)
    } yield ()
  }

  private[this] def performExchange(
      record: IssueCredentialRecord
  ): ZIO[DidComm & CredentialService, Throwable, Unit] = {
    import IssueCredentialRecord.ProtocolState._
    val aux = for {
      _ <- ZIO.logDebug(s"Running action with records => $record")

      _ <- record match {
        case IssueCredentialRecord(id, _, _, _, _, _, _, OfferPending, _, Some(offer), _, _) =>
          for {
            _ <- AgentCli.sendMessage(offer.makeMessage)
            credentialService <- ZIO.service[CredentialService]
            _ <- credentialService.markOfferSent(id)
          } yield ()

        case IssueCredentialRecord(id, _, _, _, _, _, _, RequestPending, _, _, Some(request), _) =>
          for {
            _ <- sendMessage(request.makeMessage)
            credentialService <- ZIO.service[CredentialService]
            _ <- credentialService.markRequestSent(id)
          } yield ()

        case IssueCredentialRecord(id, _, _, _, _, _, _, CredentialPending, _, _, _, Some(issue)) =>
          for {
            // TODO: decide when to send and to publish the credential !!!
            _ <- sendMessage(issue.makeMessage)
            credentialService <- ZIO.service[CredentialService]
            _ <- credentialService.markCredentialSent(id)
            _ <- credentialService.markCredentialPublicationPending(id)
          } yield ()

        case IssueCredentialRecord(id, _, _, _, _, _, _, ProblemReportPending, _, _, _, _) => ???
        case IssueCredentialRecord(id, _, _, _, _, _, _, _, _, _, _, _)                    => ZIO.unit
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
