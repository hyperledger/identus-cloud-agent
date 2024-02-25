package io.iohk.atala.agent.server.jobs

import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.walletapi.service.ManagedDIDService
import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.castor.core.service.DIDService
import io.iohk.atala.credentialstatus.controller.http.StatusListCredential
import io.iohk.atala.pollux.core.model.error.CredentialStatusListServiceError
import io.iohk.atala.pollux.core.service.CredentialStatusListService
import io.iohk.atala.pollux.vc.jwt.revocation.{BitString, VCStatusList2021, VCStatusList2021Error}
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

object StatusListJobs extends BackgroundJobsHelper {

  val syncRevocationStatuses
      : ZIO[CredentialStatusListService & DIDService & ManagedDIDService & AppConfig, Throwable, Unit] =
    for {
      credentialStatusListService <- ZIO.service[CredentialStatusListService]
      credentialStatusListsWithCreds <- credentialStatusListService.getCredentialsAndItsStatuses
        .mapError(_.toThrowable)

      updatedVcStatusListsCredsEffects = credentialStatusListsWithCreds.map { statusListWithCreds =>
        val vcStatusListCredString = statusListWithCreds.statusListCredential
        val walletAccessContext = WalletAccessContext(statusListWithCreds.walletId)

        val effect = for {
          vcStatusListCredJson <- ZIO
            .fromEither(io.circe.parser.parse(vcStatusListCredString))
            .mapError(_.underlying)
          issuer <- createJwtIssuer(statusListWithCreds.issuer, VerificationRelationship.AssertionMethod)
          vcStatusListCred <- VCStatusList2021
            .decodeFromJson(vcStatusListCredJson, issuer)
            .mapError(x => new Throwable(x.msg))
          bitString <- vcStatusListCred.getBitString.mapError(x => new Throwable(x.msg))
          encodedBeforeTmp <- bitString.encoded.mapError(x => new Throwable(x.message))
          updateBitStringEffects = statusListWithCreds.credentials.map { cred =>
            if cred.isCanceled then bitString.setRevokedInPlace(cred.statusListIndex, true)
            else ZIO.unit
          }
          _ <- ZIO
            .collectAll(updateBitStringEffects)
            .mapError(x => new Throwable(x.message))

          updatedVcStatusListCred <- vcStatusListCred.updateBitString(bitString).mapError {
            case VCStatusList2021Error.EncodingError(msg: String) => new Throwable(msg)
            case VCStatusList2021Error.DecodingError(msg: String) => new Throwable(msg)
          }
          vcStatusListCredJsonString <- updatedVcStatusListCred.toJsonWithEmbeddedProof
            .map(_.spaces2)
          _ <- credentialStatusListService
            .updateStatusListCredential(statusListWithCreds.id, vcStatusListCredJsonString)
            .mapError(_.toThrowable)
        } yield ()

        effect.provideSomeLayer(ZLayer.succeed(walletAccessContext))

      }
      config <- ZIO.service[AppConfig]
      _ <- ZIO
        .collectAll(updatedVcStatusListsCredsEffects)
        .withParallelism(config.pollux.syncRevocationStatusesBgJobProcessingParallelism)
    } yield ()
}
