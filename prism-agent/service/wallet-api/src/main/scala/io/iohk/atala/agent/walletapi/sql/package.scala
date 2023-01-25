package io.iohk.atala.agent.walletapi

import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import doobie.util.invariant.InvalidEnum
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.castor.core.model.did.{PrismDID, PrismDIDOperation, ScheduledDIDOperationStatus}
import io.iohk.atala.castor.core.model.ProtoModelHelper.*
import io.iohk.atala.prism.protos.node_models

import java.time.Instant
import scala.util.Try
import scala.collection.immutable.ArraySeq

package object sql {

  sealed trait DIDWalletStatusType
  object DIDWalletStatusType {
    case object CREATED extends DIDWalletStatusType
    case object PUBLICATION_PENDING extends DIDWalletStatusType
    case object PUBLISHED extends DIDWalletStatusType
  }

  given Meta[DIDWalletStatusType] = pgEnumString(
    "PRISM_DID_WALLET_STATUS",
    {
      case "CREATED"             => DIDWalletStatusType.CREATED
      case "PUBLICATION_PENDING" => DIDWalletStatusType.PUBLICATION_PENDING
      case "PUBLISHED"           => DIDWalletStatusType.PUBLISHED
      case s                     => throw InvalidEnum[DIDWalletStatusType](s)
    },
    {
      case DIDWalletStatusType.CREATED             => "CREATED"
      case DIDWalletStatusType.PUBLICATION_PENDING => "PUBLICATION_PENDING"
      case DIDWalletStatusType.PUBLISHED           => "PUBLISHED"
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

  final case class DIDPublicationStateRow(
      did: PrismDID,
      publicationStatus: DIDWalletStatusType,
      atalaOperationContent: Array[Byte],
      publishOperationId: Option[Array[Byte]],
      createdAt: Instant,
      updatedAt: Instant
  ) {
    def toDomain: Try[ManagedDIDState] = {
      publicationStatus match {
        case DIDWalletStatusType.CREATED => createDIDOperation.map(ManagedDIDState.Created.apply)
        case DIDWalletStatusType.PUBLICATION_PENDING =>
          for {
            createDIDOperation <- createDIDOperation
            operationId <- publishOperationId
              .toRight(RuntimeException(s"DID publication operation id does not exists for PUBLICATION_PENDING status"))
              .toTry
          } yield ManagedDIDState.PublicationPending(createDIDOperation, ArraySeq.from(operationId))
        case DIDWalletStatusType.PUBLISHED =>
          for {
            createDIDOperation <- createDIDOperation
            operationId <- publishOperationId
              .toRight(RuntimeException(s"DID publication operation id does not exists for PUBLISHED status"))
              .toTry
          } yield ManagedDIDState.Published(createDIDOperation, ArraySeq.from(operationId))
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

  object DIDPublicationStateRow {
    def from(did: PrismDID, state: ManagedDIDState, now: Instant): DIDPublicationStateRow = {
      import DIDWalletStatusType.*
      val (status, createOperation, publishedOperationId) = state match {
        case ManagedDIDState.Created(operation) => (CREATED, operation, None)
        case ManagedDIDState.PublicationPending(operation, operationId) =>
          (PUBLICATION_PENDING, operation, Some(operationId))
        case ManagedDIDState.Published(operation, operationId) => (PUBLISHED, operation, Some(operationId))
      }
      DIDPublicationStateRow(
        did = did,
        publicationStatus = status,
        atalaOperationContent = createOperation.toAtalaOperation.toByteArray,
        publishOperationId = publishedOperationId.map(_.toArray),
        createdAt = now,
        updatedAt = now
      )
    }
  }

}
