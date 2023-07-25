package io.iohk.atala.agent.walletapi

import doobie.*
import doobie.postgres.implicits.*
import doobie.util.invariant.InvalidEnum
import io.iohk.atala.agent.walletapi.model.{ManagedDIDState, PublicationState, KeyManagementMode}
import io.iohk.atala.castor.core.model.did.{PrismDID, PrismDIDOperation, ScheduledDIDOperationStatus}
import io.iohk.atala.castor.core.model.ProtoModelHelper.*
import io.iohk.atala.prism.protos.node_models

import java.time.Instant
import scala.util.Try
import scala.collection.immutable.ArraySeq
import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.castor.core.model.did.InternalKeyPurpose
import io.iohk.atala.shared.models.WalletId

package object sql {

  sealed trait PublicationStatusType
  object PublicationStatusType {
    case object CREATED extends PublicationStatusType
    case object PUBLICATION_PENDING extends PublicationStatusType
    case object PUBLISHED extends PublicationStatusType

    def from(status: PublicationState): PublicationStatusType = status match {
      case PublicationState.Created()             => CREATED
      case PublicationState.PublicationPending(_) => PUBLICATION_PENDING
      case PublicationState.Published(_)          => PUBLISHED
    }
  }

  given Meta[VerificationRelationship | InternalKeyPurpose] = pgEnumString(
    "PRISM_DID_KEY_USAGE",
    {
      case "MASTER"                => InternalKeyPurpose.Master
      case "ISSUING"               => VerificationRelationship.AssertionMethod
      case "KEY_AGREEMENT"         => VerificationRelationship.KeyAgreement
      case "AUTHENTICATION"        => VerificationRelationship.Authentication
      case "REVOCATION"            => InternalKeyPurpose.Revocation
      case "CAPABILITY_INVOCATION" => VerificationRelationship.CapabilityInvocation
      case "CAPABILITY_DELEGATION" => VerificationRelationship.CapabilityDelegation
      case s                       => throw InvalidEnum[VerificationRelationship | InternalKeyPurpose](s)
    },
    {
      case InternalKeyPurpose.Master                     => "MASTER"
      case VerificationRelationship.AssertionMethod      => "ISSUING"
      case VerificationRelationship.KeyAgreement         => "KEY_AGREEMENT"
      case VerificationRelationship.Authentication       => "AUTHENTICATION"
      case InternalKeyPurpose.Revocation                 => "REVOCATION"
      case VerificationRelationship.CapabilityInvocation => "CAPABILITY_INVOCATION"
      case VerificationRelationship.CapabilityDelegation => "CAPABILITY_DELEGATION"
    }
  )

  given Meta[KeyManagementMode] = pgEnumString(
    "PRISM_DID_KEY_MODE",
    {
      case "HD" => KeyManagementMode.HD
      case s    => throw InvalidEnum[KeyManagementMode](s)
    },
    { case KeyManagementMode.HD => "HD" }
  )

  given Meta[PublicationStatusType] = pgEnumString(
    "PRISM_DID_WALLET_STATUS",
    {
      case "CREATED"             => PublicationStatusType.CREATED
      case "PUBLICATION_PENDING" => PublicationStatusType.PUBLICATION_PENDING
      case "PUBLISHED"           => PublicationStatusType.PUBLISHED
      case s                     => throw InvalidEnum[PublicationStatusType](s)
    },
    {
      case PublicationStatusType.CREATED             => "CREATED"
      case PublicationStatusType.PUBLICATION_PENDING => "PUBLICATION_PENDING"
      case PublicationStatusType.PUBLISHED           => "PUBLISHED"
    }
  )

  given Meta[ScheduledDIDOperationStatus] = pgEnumString(
    "PRISM_DID_OPERATION_STATUS",
    {
      case "PENDING_SUBMISSION"     => ScheduledDIDOperationStatus.Pending
      case "AWAIT_CONFIRMATION"     => ScheduledDIDOperationStatus.AwaitingConfirmation
      case "CONFIRMED_AND_APPLIED"  => ScheduledDIDOperationStatus.Confirmed
      case "CONFIRMED_AND_REJECTED" => ScheduledDIDOperationStatus.Rejected
      case s                        => throw InvalidEnum[ScheduledDIDOperationStatus](s)
    },
    {
      case ScheduledDIDOperationStatus.Pending              => "PENDING_SUBMISSION"
      case ScheduledDIDOperationStatus.AwaitingConfirmation => "AWAIT_CONFIRMATION"
      case ScheduledDIDOperationStatus.Confirmed            => "CONFIRMED_AND_APPLIED"
      case ScheduledDIDOperationStatus.Rejected             => "CONFIRMED_AND_REJECTED"
    }
  )

  given prismDIDGet: Get[PrismDID] = Get[String].map(PrismDID.fromString(_).left.map(Exception(_)).toTry.get)
  given prismDIDPut: Put[PrismDID] = Put[String].contramap(_.asCanonical.toString)

  given arraySeqByteGet: Get[ArraySeq[Byte]] = Get[Array[Byte]].map(ArraySeq.from)
  given arraySeqBytePut: Put[ArraySeq[Byte]] = Put[Array[Byte]].contramap(_.toArray)

  given walletIdGet: Get[WalletId] = Get[Int].map(WalletId.fromInt)
  given walletIdPut: Put[WalletId] = Put[Int].contramap(_.toInt)

  final case class DIDStateRow(
      did: PrismDID,
      publicationStatus: PublicationStatusType,
      atalaOperationContent: Array[Byte],
      publishOperationId: Option[Array[Byte]],
      createdAt: Instant,
      updatedAt: Instant,
      keyMode: KeyManagementMode,
      didIndex: Int,
      walletId: WalletId
  ) {
    def toDomain: Try[ManagedDIDState] = {
      publicationStatus match {
        case PublicationStatusType.CREATED =>
          createDIDOperation.map(op => ManagedDIDState(op, didIndex, PublicationState.Created()))
        case PublicationStatusType.PUBLICATION_PENDING =>
          for {
            createDIDOperation <- createDIDOperation
            operationId <- publishOperationId
              .toRight(RuntimeException(s"DID publication operation id does not exists for PUBLICATION_PENDING status"))
              .toTry
          } yield ManagedDIDState(
            createDIDOperation,
            didIndex,
            PublicationState.PublicationPending(ArraySeq.from(operationId))
          )
        case PublicationStatusType.PUBLISHED =>
          for {
            createDIDOperation <- createDIDOperation
            operationId <- publishOperationId
              .toRight(RuntimeException(s"DID publication operation id does not exists for PUBLISHED status"))
              .toTry
          } yield ManagedDIDState(createDIDOperation, didIndex, PublicationState.Published(ArraySeq.from(operationId)))
      }
    }

    private def createDIDOperation: Try[PrismDIDOperation.Create] = {
      Try(node_models.AtalaOperation.parseFrom(atalaOperationContent))
        .flatMap { atalaOperation =>
          atalaOperation.operation.createDid
            .toRight(
              s"cannot extract CreateDIDOperation from AtalaOperation (${atalaOperation.operation.getClass.getSimpleName} found)"
            )
            .flatMap(_.toDomain)
            .left
            .map(RuntimeException(_))
            .toTry
        }
    }
  }

  object DIDStateRow {
    def from(did: PrismDID, state: ManagedDIDState, now: Instant, walletId: WalletId): DIDStateRow = {
      val createOperation = state.createOperation
      val status = PublicationStatusType.from(state.publicationState)
      val publishedOperationId = state.publicationState match {
        case PublicationState.Created()                       => None
        case PublicationState.PublicationPending(operationId) => Some(operationId.toArray)
        case PublicationState.Published(operationId)          => Some(operationId.toArray)
      }
      DIDStateRow(
        did = did,
        publicationStatus = status,
        atalaOperationContent = createOperation.toAtalaOperation.toByteArray,
        publishOperationId = publishedOperationId.map(_.toArray),
        createdAt = now,
        updatedAt = now,
        keyMode = state.keyMode,
        didIndex = state.didIndex,
        walletId = walletId
      )
    }
  }

}
