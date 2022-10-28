package io.iohk.atala.agent.server.jobs

import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import io.iohk.atala.pollux.core.model.error.CreateCredentialPayloadFromRecordError
import io.iohk.atala.pollux.core.model.error.IssueCredentialError
import io.iohk.atala.pollux.core.service.CredentialService
import io.iohk.atala.pollux.vc.jwt.W3cCredentialPayload
import zio.*

import java.time.Instant

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

  private[this] def performExchange(record: IssueCredentialRecord) = {
    Console.printLine(s"Running action with record => $record")
  }

  val publishCredentialsToDlt = {
    for {
      credentialService <- ZIO.service[CredentialService]
      _ <- performPublishCredentialsToDlt(credentialService)
    } yield ()

  }

  private[this] def performPublishCredentialsToDlt(credentialService: CredentialService) = {


    val res: ZIO[Any, IssueCredentialError | CreateCredentialPayloadFromRecordError, Seq[W3cCredentialPayload]] = for {
      records <- credentialService.getCredentialRecordsByState(IssueCredentialRecord.State.CredentialPending)
      credentials <- ZIO.foreach(records) {
        record => credentialService.createCredentialPayloadFromRecord(record, credentialService.createIssuer, Instant.now())
      }
    } yield credentials


    ZIO.unit
  }

}
