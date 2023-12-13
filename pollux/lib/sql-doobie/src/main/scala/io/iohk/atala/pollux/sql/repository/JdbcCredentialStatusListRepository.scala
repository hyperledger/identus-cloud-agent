package io.iohk.atala.pollux.sql.repository

import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import io.iohk.atala.pollux.vc.jwt.{Issuer, StatusPurpose}
import io.iohk.atala.pollux.vc.jwt.revocation.{BitString, BitStringError, VCStatusList2021}
//import doobie.implicits.legacy.instant.* TODO: might need for Instance Meta
import io.iohk.atala.castor.core.model.did.*
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.core.repository.CredentialStatusListRepository
import io.iohk.atala.shared.db.ContextAwareTask
import io.iohk.atala.shared.db.Implicits.*
import io.iohk.atala.pollux.vc.jwt.revocation.BitStringError.*
import zio.*
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}

import java.time.Instant
import java.util.UUID

class JdbcCredentialStatusListRepository(xa: Transactor[ContextAwareTask], xb: Transactor[Task])
    extends CredentialStatusListRepository {
  def getLatestOfTheWallet: RIO[WalletAccessContext, Option[CredentialStatusList]] = {

    val cxnIO =
      sql"""
           | SELECT
           |   id,
           |   wallet_id,
           |   issuer,
           |   issued,
           |   purpose,
           |   status_list_jwt_credential,
           |   size,
           |   last_used_index,
           |   created_at,
           |   updated_at
           |  FROM public.credential_status_lists order by created_at DESC limit 1
           |""".stripMargin
        .query[CredentialStatusList]
        .option

    cxnIO
      .transactWallet(xa)

  }

  def createNewForTheWallet(jwtIssuer: Issuer): RIO[WalletAccessContext, CredentialStatusList] = {

    val id = UUID.randomUUID()
    val issued = Instant.now()
    val issuerDid = jwtIssuer.did.value

    val encodedJwtCredential = for {
      bitString <- BitString.getInstance().mapError {
        case InvalidSize(message)      => new Throwable(message)
        case EncodingError(message)    => new Throwable(message)
        case DecodingError(message)    => new Throwable(message)
        case IndexOutOfBounds(message) => new Throwable(message)
      }
      emptyJwtCredential <- VCStatusList2021
        .build(
          vcId = s"https://example.com/credentials/status/$id", // TODO: change URL to real one
          slId = "",
          revocationData = bitString,
          jwtIssuer = jwtIssuer
        )
        .mapError(x => new Throwable(x.msg))

      encodedJwtCredential <- emptyJwtCredential.encoded
    } yield encodedJwtCredential

    for {
      jwtCredential <- encodedJwtCredential
      query = sql"""
                   |INSERT INTO public.credential_status_lists (
                   |  id,
                   |  issuer,
                   |  issued,
                   |  purpose,
                   |  status_list_jwt_credential,
                   |  size,
                   |  last_used_index,
                   |  wallet_id
                   | )
                   |VALUES (
                   |  $id,
                   |  $issuerDid,
                   |  $issued,
                   |  ${StatusPurpose.Revocation}::public.enum_credential_status_list_purpose,
                   |  ${jwtCredential.value},
                   |  ${BitString.MIN_SL2021_SIZE},
                   |  0,
                   |  current_setting('app.current_wallet_id')::UUID
                   | )
                   |RETURNING id, wallet_id, issuer, issued, purpose, status_list_jwt_credential, size, last_used_index, created_at, updated_at
             """.stripMargin.query[CredentialStatusList].unique
      newStatusList <- query
        .transactWallet(xa)
    } yield newStatusList

  }

  def allocateSpaceForCredential(
      issueCredentialRecordId: DidCommID,
      credentialStatusListId: UUID,
      statusListIndex: Int
  ): RIO[WalletAccessContext, Unit] = {

    val statusListEntryCreationQuery =
      sql"""
           | INSERT INTO public.credentials_in_status_list (
           |   id,
           |   issue_credential_record_id,
           |   credential_status_list_id,
           |   status_list_index,
           |   is_canceled
           | )
           | VALUES (
           |   ${UUID.randomUUID()},
           |   $issueCredentialRecordId,
           |   $credentialStatusListId,
           |   $statusListIndex,
           |   false
           | )
           |""".stripMargin.update.run

    val statusListUpdateQuery =
      sql"""
           | UPDATE public.credential_status_lists
           | SET
           |   last_used_index = $statusListIndex,
           |   updated_at = ${Instant.now()}
           | WHERE
           |   id = $credentialStatusListId
           |""".stripMargin.update.run

    val res: ConnectionIO[Unit] = for {
      _ <- statusListEntryCreationQuery
      _ <- statusListUpdateQuery
    } yield ()

    res.transactWallet(xa)

  }

}

object JdbcCredentialStatusListRepository {
  val layer: URLayer[Transactor[ContextAwareTask] & Transactor[Task], CredentialStatusListRepository] =
    ZLayer.fromFunction(new JdbcCredentialStatusListRepository(_, _))
}
