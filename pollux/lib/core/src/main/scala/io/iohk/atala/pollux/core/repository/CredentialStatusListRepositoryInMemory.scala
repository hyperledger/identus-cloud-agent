package io.iohk.atala.pollux.core.repository

import io.iohk.atala.pollux.core.model.CredentialStatusList
import io.iohk.atala.pollux.vc.jwt.{Issuer, StatusPurpose, revocation}
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import zio.*
import io.iohk.atala.pollux.core.model.*
import io.iohk.atala.pollux.vc.jwt.revocation.BitStringError.{
  DecodingError,
  EncodingError,
  IndexOutOfBounds,
  InvalidSize
}
import io.iohk.atala.castor.core.model.did.{CanonicalPrismDID, PrismDID}
import io.iohk.atala.pollux.vc.jwt.revocation.{BitString, VCStatusList2021}

import java.time.Instant
import java.util.UUID

class CredentialStatusListRepositoryInMemory(
    walletToStatusListRefs: Ref[Map[WalletId, Ref[Map[UUID, CredentialStatusList]]]],
    statusListToCredInStatusListRefs: Ref[Map[UUID, Ref[Map[UUID, CredentialInStatusList]]]]
) extends CredentialStatusListRepository {

  private def walletToStatusListStorageRefs: URIO[WalletAccessContext, Ref[Map[UUID, CredentialStatusList]]] =
    for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      refs <- walletToStatusListRefs.get
      maybeWalletRef = refs.get(walletId)
      walletRef <- maybeWalletRef
        .fold {
          for {
            ref <- Ref.make(Map.empty[UUID, CredentialStatusList])
            _ <- walletToStatusListRefs.set(refs.updated(walletId, ref))
          } yield ref
        }(ZIO.succeed)
    } yield walletRef

  private def statusListToCredInStatusListStorageRefs(
      statusListId: UUID
  ): Task[Ref[Map[UUID, CredentialInStatusList]]] =
    for {
      refs <- statusListToCredInStatusListRefs.get
      maybeStatusListIdRef = refs.get(statusListId)
      statusListIdRef <- maybeStatusListIdRef.fold {
        for {
          ref <- Ref.make(Map.empty[UUID, CredentialInStatusList])
          _ <- statusListToCredInStatusListRefs.set(refs.updated(statusListId, ref))
        } yield ref
      }(ZIO.succeed)
    } yield statusListIdRef

  def findById(id: UUID): Task[Option[CredentialStatusList]] = for {
    refs <- walletToStatusListRefs.get
    stores <- ZIO.foreach(refs.values.toList)(_.get)
    found = stores.flatMap(_.values).find(_.id == id)
  } yield found

  def getLatestOfTheWallet: RIO[WalletAccessContext, Option[CredentialStatusList]] = for {
    storageRef <- walletToStatusListStorageRefs
    storage <- storageRef.get
    latest = storage.toSeq
      .sortBy(_._2.createdAt) { (x, y) => if x.isAfter(y) then -1 else 1 /* DESC */ }
      .headOption
      .map(_._2)
  } yield latest

  def createNewForTheWallet(
      jwtIssuer: Issuer,
      statusListRegistryUrl: String
  ): RIO[WalletAccessContext, CredentialStatusList] = {

    val id = UUID.randomUUID()
    val issued = Instant.now()
    val issuerDid = jwtIssuer.did.value
    val canonical = PrismDID.fromString(issuerDid).fold(e => throw RuntimeException(e), _.asCanonical)

    val encodedJwtCredential = for {
      bitString <- BitString.getInstance().mapError {
        case InvalidSize(message)      => new Throwable(message)
        case EncodingError(message)    => new Throwable(message)
        case DecodingError(message)    => new Throwable(message)
        case IndexOutOfBounds(message) => new Throwable(message)
      }
      emptyJwtCredential <- VCStatusList2021
        .build(
          vcId = s"$statusListRegistryUrl/credential-status/$id",
          slId = "",
          revocationData = bitString,
          jwtIssuer = jwtIssuer
        )
        .mapError(x => new Throwable(x.msg))

      encodedJwtCredential <- emptyJwtCredential.encoded
    } yield encodedJwtCredential

    for {
      credential <- encodedJwtCredential
      storageRef <- walletToStatusListStorageRefs
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      newCredentialStatusList = CredentialStatusList(
        id = id,
        walletId = walletId,
        issuer = canonical,
        issued = issued,
        purpose = StatusPurpose.Revocation,
        statusListJwtCredential = credential.value,
        size = BitString.MIN_SL2021_SIZE,
        lastUsedIndex = 0,
        createdAt = Instant.now(),
        updatedAt = None
      )
      _ <- storageRef.update(r => r + (newCredentialStatusList.id -> newCredentialStatusList))
    } yield newCredentialStatusList

  }

  def allocateSpaceForCredential(
      issueCredentialRecordId: DidCommID,
      credentialStatusListId: UUID,
      statusListIndex: Int
  ): RIO[WalletAccessContext, Unit] = {
    val newCredentialInStatusList = CredentialInStatusList(
      id = UUID.randomUUID(),
      issueCredentialRecordId = issueCredentialRecordId,
      credentialStatusListId = credentialStatusListId,
      statusListIndex = statusListIndex,
      isCanceled = false,
      createdAt = Instant.now(),
      updatedAt = None
    )

    for {
      credentialInStatusListStorageRef <- statusListToCredInStatusListStorageRefs(credentialStatusListId)
      _ <- credentialInStatusListStorageRef.update(r => r + (newCredentialInStatusList.id -> newCredentialInStatusList))
      walletToStatusListStorageRef <- walletToStatusListStorageRefs
      _ <- walletToStatusListStorageRef.update(r => {
        val value = r.get(credentialStatusListId)
        value.fold(r) { v =>
          val updated = v.copy(lastUsedIndex = statusListIndex, updatedAt = Some(Instant.now))
          r.updated(credentialStatusListId, updated)
        }
      })
    } yield ()

  }

}

object CredentialStatusListRepositoryInMemory {
  val layer: ULayer[CredentialStatusListRepositoryInMemory] = ZLayer.fromZIO(
    for {
      walletToStatusList <- Ref
        .make(Map.empty[WalletId, Ref[Map[UUID, CredentialStatusList]]])
      statusListIdToCredInStatusList <- Ref.make(Map.empty[UUID, Ref[Map[UUID, CredentialInStatusList]]])
    } yield CredentialStatusListRepositoryInMemory(walletToStatusList, statusListIdToCredInStatusList)
  )
}

private case class CredentialInStatusList(
    id: UUID,
    issueCredentialRecordId: DidCommID,
    credentialStatusListId: UUID,
    statusListIndex: Int,
    isCanceled: Boolean,
    createdAt: Instant = Instant.now(),
    updatedAt: Option[Instant] = None
)
