package io.iohk.atala.castor.core.service

import io.iohk.atala.castor.core.model.did.{
  CanonicalPrismDID,
  DIDData,
  DIDMetadata,
  InternalPublicKey,
  LongFormPrismDID,
  PrismDID,
  PublicKey,
  ScheduleDIDOperationOutcome,
  ScheduledDIDOperationDetail,
  SignedPrismDIDOperation
}
import zio.*
import io.iohk.atala.castor.core.model.ProtoModelHelper
import io.iohk.atala.castor.core.model.error.{DIDOperationError, DIDResolutionError}
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.shared.models.HexString
import io.iohk.atala.prism.protos.{node_api, node_models}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeService
import io.iohk.atala.prism.protos.node_models.OperationOutput.OperationMaybe

import scala.collection.immutable.ArraySeq
import io.iohk.atala.castor.core.model.error.OperationValidationError

trait DIDService {
  def scheduleOperation(operation: SignedPrismDIDOperation): IO[DIDOperationError, ScheduleDIDOperationOutcome]
  def getScheduledDIDOperationDetail(
      operationId: Array[Byte]
  ): IO[DIDOperationError, Option[ScheduledDIDOperationDetail]]
  def resolveDID(did: PrismDID): IO[DIDResolutionError, Option[(DIDMetadata, DIDData)]]
}

object DIDServiceImpl {
  val layer: URLayer[NodeService & DIDOperationValidator, DIDService] =
    ZLayer.fromFunction(DIDServiceImpl(_, _))
}

private class DIDServiceImpl(didOpValidator: DIDOperationValidator, nodeClient: NodeService)
    extends DIDService,
      ProtoModelHelper {

  override def scheduleOperation(
      signedOperation: SignedPrismDIDOperation
  ): IO[DIDOperationError, ScheduleDIDOperationOutcome] = {
    val operationRequest = node_api.ScheduleOperationsRequest(
      signedOperations = Seq(signedOperation.toProto)
    )
    for {
      _ <- ZIO
        .fromEither(didOpValidator.validate(signedOperation.operation))
        .mapError(DIDOperationError.ValidationError.apply)
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
    val request = node_api.GetDidDocumentRequest(did = canonicalDID.toString)
    for {
      unpublishedDidData <- did match {
        case _: CanonicalPrismDID => ZIO.none
        case d: LongFormPrismDID  => extractUnpublishedDIDData(d).asSome
      }
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
                  canonicalId =
                    unpublishedDidData.map(_ => canonicalDID), // only shows canonicalId if long-form and published
                  deactivated = didData.internalKeys.isEmpty && didData.publicKeys.isEmpty
                )
                metadata -> didData
              }
              .asSome
        )
    } yield publishedDidData.orElse(unpublishedDidData)
  }

  private def extractUnpublishedDIDData(did: LongFormPrismDID): IO[DIDResolutionError, (DIDMetadata, DIDData)] = {
    ZIO
      .fromEither(did.createOperation)
      .mapError(e => DIDResolutionError.ValidationError(OperationValidationError.InvalidArgument(e)))
      .flatMap { op =>
        // unpublished CreateOperation (if exists) must be validated before the resolution
        ZIO
          .fromEither(didOpValidator.validate(op))
          .mapError(DIDResolutionError.ValidationError.apply)
          .as(op)
      }
      .map { op =>
        val metadata =
          DIDMetadata(
            lastOperationHash = ArraySeq.from(did.stateHash.toByteArray),
            canonicalId = None, // unpublished DID must not contain canonicalId
            deactivated = false // unpublished DID cannot be deactivated
          )
        val didData = DIDData(
          id = did.asCanonical,
          publicKeys = op.publicKeys.collect { case pk: PublicKey => pk },
          services = op.services,
          internalKeys = op.publicKeys.collect { case pk: InternalPublicKey => pk },
          context = op.context
        )
        metadata -> didData
      }
  }

}
