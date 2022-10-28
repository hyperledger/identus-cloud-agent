package io.iohk.atala.agent.server.jobs

import io.iohk.atala.pollux.core.model.IssueCredentialRecord
import io.iohk.atala.pollux.core.model.error.CreateCredentialPayloadFromRecordError
import io.iohk.atala.pollux.core.model.error.IssueCredentialError
import io.iohk.atala.pollux.core.model.error.PublishCredentialBatchError
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
    type PublishToDltError = IssueCredentialError | CreateCredentialPayloadFromRecordError | PublishCredentialBatchError

    val res: ZIO[Any, PublishToDltError, Seq[
      W3cCredentialPayload
    ]] = for {
      records <- credentialService.getCredentialRecordsByState(IssueCredentialRecord.State.CredentialPending)
      // NOTE: the line below is a potentially slow operation, because <createCredentialPayloadFromRecord> makes a database SELECT call,
      // so calling this function n times will make n database SELECT calls, while it can be optimized to get
      // all data in one query, this function here has to be refactored as well. Consider doing this if this job is too slow
      credentials <- ZIO.foreach(records) { record =>
        credentialService.createCredentialPayloadFromRecord(record, credentialService.createIssuer, Instant.now())
      }
      // FIXME: issuer here should come from castor not from credential service, this needs to be done before going to prod
      publishedBatchData <- credentialService.publishCredentialBatch(credentials, credentialService.createIssuer)
    } yield credentials

    ZIO.unit
  }

}
