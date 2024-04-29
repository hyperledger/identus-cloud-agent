package org.hyperledger.identus.pollux.sql.repository

import doobie.*
import doobie.implicits.*
import doobie.postgres.*
import doobie.postgres.implicits.*
import org.hyperledger.identus.pollux.vc.jwt.{Issuer, StatusPurpose}
import org.hyperledger.identus.pollux.vc.jwt.revocation.{BitString, BitStringError, VCStatusList2021}
import org.hyperledger.identus.castor.core.model.did.*
import org.hyperledger.identus.pollux.core.model.*
import org.hyperledger.identus.pollux.core.repository.CredentialStatusListRepository
import org.hyperledger.identus.shared.db.ContextAwareTask
import org.hyperledger.identus.shared.db.Implicits.*
import org.hyperledger.identus.pollux.vc.jwt.revocation.BitStringError.*
import zio.*
import zio.interop.catz.*
import org.hyperledger.identus.shared.models.{WalletAccessContext, WalletId}

import java.time.Instant
import java.util.UUID

class JdbcCredentialStatusListRepository(xa: Transactor[ContextAwareTask], xb: Transactor[Task])
    extends CredentialStatusListRepository {

  def findById(id: UUID): Task[Option[CredentialStatusList]] = {
    val cxnIO =
      sql"""
           | SELECT
           |   id,
           |   wallet_id,
           |   issuer,
           |   issued,
           |   purpose,
           |   status_list_credential,
           |   size,
           |   last_used_index,
           |   created_at,
           |   updated_at
           |  FROM public.credential_status_lists where id = $id
           |""".stripMargin
        .query[CredentialStatusList]
        .option

    cxnIO.transact(xb)

  }

  def getLatestOfTheWallet: RIO[WalletAccessContext, Option[CredentialStatusList]] = {

    val cxnIO =
      sql"""
           | SELECT
           |   id,
           |   wallet_id,
           |   issuer,
           |   issued,
           |   purpose,
           |   status_list_credential,
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

  def createNewForTheWallet(
      jwtIssuer: Issuer,
      statusListRegistryUrl: String
  ): RIO[WalletAccessContext, CredentialStatusList] = {

    val id = UUID.randomUUID()
    val issued = Instant.now()
    val issuerDid = jwtIssuer.did.value

    val credentialWithEmbeddedProof = for {
      bitString <- BitString.getInstance().mapError {
        case InvalidSize(message)      => new Throwable(message)
        case EncodingError(message)    => new Throwable(message)
        case DecodingError(message)    => new Throwable(message)
        case IndexOutOfBounds(message) => new Throwable(message)
      }
      emptyStatusListCredential <- VCStatusList2021
        .build(
          vcId = s"$statusListRegistryUrl/credential-status/$id",
          slId = "",
          revocationData = bitString,
          jwtIssuer = jwtIssuer
        )
        .mapError(x => new Throwable(x.msg))

      credentialWithEmbeddedProof <- emptyStatusListCredential.toJsonWithEmbeddedProof
    } yield credentialWithEmbeddedProof.spaces2

    for {
      credentialStr <- credentialWithEmbeddedProof
      query = sql"""
                   |INSERT INTO public.credential_status_lists (
                   |  id,
                   |  issuer,
                   |  issued,
                   |  purpose,
                   |  status_list_credential,
                   |  size,
                   |  last_used_index,
                   |  wallet_id
                   | )
                   |VALUES (
                   |  $id,
                   |  $issuerDid,
                   |  $issued,
                   |  ${StatusPurpose.Revocation}::public.enum_credential_status_list_purpose,
                   |  $credentialStr::JSON,
                   |  ${BitString.MIN_SL2021_SIZE},
                   |  0,
                   |  current_setting('app.current_wallet_id')::UUID
                   | )
                   |RETURNING id, wallet_id, issuer, issued, purpose, status_list_credential, size, last_used_index, created_at, updated_at
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

  def revokeByIssueCredentialRecordId(
      issueCredentialRecordId: DidCommID
  ): RIO[WalletAccessContext, Boolean] = {

    for {
      walletId <- ZIO.service[WalletAccessContext].map(_.walletId)
      updateQuery =
        sql"""
             | UPDATE public.credentials_in_status_list AS cisl
             | SET is_canceled = true
             | FROM public.credential_status_lists AS csl
             | WHERE cisl.credential_status_list_id = csl.id
             | AND csl.wallet_id = ${walletId.toUUID}
             | AND cisl.issue_credential_record_id = $issueCredentialRecordId
             | AND cisl.is_canceled = false;
             |""".stripMargin.update.run

      revoked <- updateQuery.transactWallet(xa).map(_ > 0)

    } yield revoked

  }

  def getCredentialStatusListsWithCreds: Task[List[CredentialStatusListWithCreds]] = {

    // Might need to add wallet Id in the select query, because I'm selecting all of them
    val cxnIO =
      sql"""
           | SELECT
           |   csl.id as credential_status_list_id,
           |   csl.issuer,
           |   csl.issued,
           |   csl.purpose,
           |   csl.wallet_id,
           |   csl.status_list_credential,
           |   csl.size,
           |   csl.last_used_index,
           |   cisl.id as credential_in_status_list_id,
           |   cisl.issue_credential_record_id,
           |   cisl.status_list_index,
           |   cisl.is_canceled,
           |   cisl.is_processed
           |  FROM public.credential_status_lists csl
           |  LEFT JOIN public.credentials_in_status_list cisl ON csl.id = cisl.credential_status_list_id
           |""".stripMargin
        .query[CredentialStatusListWithCred]
        .to[List]

    val credentialStatusListsWithCredZio = cxnIO
      .transact(xb)

    for {
      credentialStatusListsWithCred <- credentialStatusListsWithCredZio
    } yield {
      credentialStatusListsWithCred
        .groupBy(_.credentialStatusListId)
        .map { case (id, items) =>
          CredentialStatusListWithCreds(
            id,
            items.head.walletId,
            items.head.issuer,
            items.head.issued,
            items.head.purpose,
            items.head.statusListCredential,
            items.head.size,
            items.head.lastUsedIndex,
            items.map { item =>
              CredInStatusList(
                item.credentialInStatusListId,
                item.issueCredentialRecordId,
                item.statusListIndex,
                item.isCanceled,
                item.isProcessed,
              )
            }
          )
        }
        .toList
    }
  }

  def updateStatusListCredential(
      credentialStatusListId: UUID,
      statusListCredential: String
  ): RIO[WalletAccessContext, Unit] = {

    val updateQuery =
      sql"""
           | UPDATE public.credential_status_lists
           | SET
           |   status_list_credential = $statusListCredential::JSON,
           |   updated_at = ${Instant.now()}
           | WHERE
           |   id = $credentialStatusListId
           |""".stripMargin.update.run

    updateQuery.transactWallet(xa).unit

  }

  def markAsProcessedMany(
      credsInStatusListIds: Seq[UUID]
  ): RIO[WalletAccessContext, Unit] = {

    val updateQuery =
      sql"""
           | UPDATE public.credentials_in_status_list
           | SET
           |   is_processed = true
           | WHERE
           |   id::text IN (${credsInStatusListIds.map(_.toString).mkString(",")})
           |""".stripMargin.update.run

    if credsInStatusListIds.nonEmpty then updateQuery.transactWallet(xa).unit else ZIO.unit

  }

}

object JdbcCredentialStatusListRepository {
  val layer: URLayer[Transactor[ContextAwareTask] & Transactor[Task], CredentialStatusListRepository] =
    ZLayer.fromFunction(new JdbcCredentialStatusListRepository(_, _))
}
