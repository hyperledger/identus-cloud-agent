package io.iohk.atala.castor.core.service

import io.iohk.atala.castor.core.model.did.{
  CanonicalPrismDID,
  DIDData,
  DIDMetadata,
  LongFormPrismDID,
  PrismDID,
  PrismDIDOperation,
  ScheduleDIDOperationOutcome,
  ScheduledDIDOperationDetail,
  SignedPrismDIDOperation
}
import zio.*
import io.iohk.atala.castor.core.model.ProtoModelHelper
import io.iohk.atala.castor.core.model.error.{DIDOperationError, DIDResolutionError}
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.shared.utils.Traverse.*
import io.iohk.atala.prism.protos.{node_api, node_models}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeServiceStub
import io.iohk.atala.prism.protos.node_models.OperationOutput.{OperationMaybe, Result}

import scala.collection.immutable.ArraySeq

trait DIDService {
  def scheduleOperation(operation: SignedPrismDIDOperation): IO[DIDOperationError, ScheduleDIDOperationOutcome]
  def getScheduledDIDOperationDetail(
      operationId: Array[Byte]
  ): IO[DIDOperationError, Option[ScheduledDIDOperationDetail]]
  def resolveDID(did: PrismDID): IO[DIDResolutionError, Option[(DIDMetadata, DIDData)]]
}

object DIDServiceImpl {
  val layer: URLayer[NodeServiceStub & DIDOperationValidator, DIDService] =
    ZLayer.fromFunction(DIDServiceImpl(_, _))
}

private class DIDServiceImpl(didOpValidator: DIDOperationValidator, nodeClient: NodeServiceStub)
    extends DIDService,
      ProtoModelHelper {

  override def scheduleOperation(
      signedOperation: SignedPrismDIDOperation
  ): IO[DIDOperationError, ScheduleDIDOperationOutcome] = {
    val operationRequest = node_api.ScheduleOperationsRequest(
      signedOperations = Seq(signedOperation.toProto)
    )
    for {
      _ <- ZIO.fromEither(didOpValidator.validate(signedOperation.operation))
      operationOutput <- ZIO
        .fromFuture(_ => nodeClient.scheduleOperations(operationRequest))
        .mapBoth(DIDOperationError.DLTProxyError.apply, _.outputs.toList)
        .map {
          case output :: Nil => Right(output)
          case _ => Left(DIDOperationError.UnexpectedDLTResult("operation result is expected to have exactly 1 output"))
        }
        .absolve
      operationId <- ZIO.fromEither {
        operationOutput.operationMaybe match {
          case OperationMaybe.OperationId(id) => Right(id.toByteArray)
          case OperationMaybe.Empty =>
            Left(DIDOperationError.UnexpectedDLTResult("operation result does not contain operation detail"))
          case OperationMaybe.Error(e) =>
            Left(DIDOperationError.UnexpectedDLTResult(s"operation result was not successful: $e"))
        }
      }
    } yield ScheduleDIDOperationOutcome(
      did = signedOperation.operation.did,
      operation = signedOperation.operation,
      operationId = ArraySeq.from(operationId)
    )
  }

  override def getScheduledDIDOperationDetail(
      operationId: Array[Byte]
  ): IO[DIDOperationError, Option[ScheduledDIDOperationDetail]] = {
    for {
      result <- ZIO
        .fromFuture(_ => nodeClient.getOperationInfo(node_api.GetOperationInfoRequest(operationId.toProto)))
        .mapError(DIDOperationError.DLTProxyError.apply)
      detail <- ZIO
        .fromEither(result.toDomain)
        .mapError(DIDOperationError.UnexpectedDLTResult.apply)
    } yield detail
  }

  override def resolveDID(did: PrismDID): IO[DIDResolutionError, Option[(DIDMetadata, DIDData)]] = {
    val canonicalDID = did.asCanonical
    val createOperation = did match {
      case LongFormPrismDID(createOperation) => Some(createOperation)
      case _: CanonicalPrismDID              => None
    }

    val unpublishedDidData = createOperation.map { op =>
      val metadata =
        DIDMetadata(
          lastOperationHash = ArraySeq.from(PrismDID.buildLongFormFromOperation(op).stateHash.toByteArray),
          deactivated = false // unpublished DID cannot be deactivated
        )
      val didData = DIDData(
        id = did, // id must match the DID that was resolved: https://www.w3.org/TR/did-core/#dfn-diddocument
        publicKeys = op.publicKeys,
        services = op.services,
        internalKeys = op.internalKeys
      )
      metadata -> didData
    }

    val request = node_api.GetDidDocumentRequest(did = canonicalDID.toString)
    for {
      result <- ZIO
        .fromFuture(_ => nodeClient.getDidDocument(request))
        .mapError(DIDResolutionError.DLTProxyError.apply)
      publishedDidData <- ZIO
        .fromOption(result.document)
        .foldZIO(
          _ => ZIO.none,
          didDataProto =>
            didDataProto.filterRevokedKeysAndServices
              .flatMap(didData => ZIO.fromEither(didData.toDomain))
              .mapError(DIDResolutionError.UnexpectedDLTResult.apply)
              .map { didData =>
                val metadata = DIDMetadata(
                  lastOperationHash = ArraySeq.from(result.lastUpdateOperation.toByteArray),
                  deactivated = didData.internalKeys.isEmpty && didData.publicKeys.isEmpty
                )
                metadata -> didData
              }
              .asSome
        )
    } yield publishedDidData.orElse(unpublishedDidData)
  }

}
