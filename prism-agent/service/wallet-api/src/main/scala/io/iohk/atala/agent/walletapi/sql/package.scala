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

package object sql {

  sealed trait PublicationStatusType
  object PublicationStatusType {
    case object CREATED extends PublicationStatusType
    case object PUBLICATION_PENDING extends PublicationStatusType
    case object PUBLISHED extends PublicationStatusType
  }

  given Meta[KeyManagementMode] = pgEnumString(
    "PRISM_DID_KEY_MODE",
    {
      case "HD"     => KeyManagementMode.HD
      case "RANDOM" => KeyManagementMode.Random
      case s        => throw InvalidEnum[KeyManagementMode](s)
    },
    {
      case KeyManagementMode.HD     => "HD"
      case KeyManagementMode.Random => "RANDOM"
    }
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

  final case class DIDStateRow(
      did: PrismDID,
      publicationStatus: PublicationStatusType,
      atalaOperationContent: Array[Byte],
      publishOperationId: Option[Array[Byte]],
      createdAt: Instant,
      updatedAt: Instant,
      keyMode: KeyManagementMode
  ) {
    def toDomain: Try[ManagedDIDState] = {
      publicationStatus match {
        case PublicationStatusType.CREATED =>
          createDIDOperation.map(op => ManagedDIDState(op, ???, PublicationState.Created()))
        case PublicationStatusType.PUBLICATION_PENDING =>
          for {
            createDIDOperation <- createDIDOperation
            operationId <- publishOperationId
              .toRight(RuntimeException(s"DID publication operation id does not exists for PUBLICATION_PENDING status"))
              .toTry
          } yield ManagedDIDState(
            createDIDOperation,
            ???,
            PublicationState.PublicationPending(ArraySeq.from(operationId))
          )
        case PublicationStatusType.PUBLISHED =>
          for {
            createDIDOperation <- createDIDOperation
            operationId <- publishOperationId
              .toRight(RuntimeException(s"DID publication operation id does not exists for PUBLISHED status"))
              .toTry
          } yield ManagedDIDState(createDIDOperation, ???, PublicationState.Published(ArraySeq.from(operationId)))
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
    def from(did: PrismDID, state: ManagedDIDState, now: Instant): DIDStateRow = {
      import PublicationStatusType.*
      val createOperation = state.createOperation
      val (status, publishedOperationId) = state.publicationState match {
        case PublicationState.Created()                       => (CREATED, None)
        case PublicationState.PublicationPending(operationId) => (PUBLICATION_PENDING, Some(operationId.toArray))
        case PublicationState.Published(operationId)          => (PUBLISHED, Some(operationId.toArray))
      }
      DIDStateRow(
        did = did,
        publicationStatus = status,
        atalaOperationContent = createOperation.toAtalaOperation.toByteArray,
        publishOperationId = publishedOperationId.map(_.toArray),
        createdAt = now,
        updatedAt = now,
        keyMode = state.keyMode
      )
    }
  }

}
