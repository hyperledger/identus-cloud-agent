package io.iohk.atala.castor.core.service

import io.iohk.atala.castor.core.model.did.{
  PrismDID,
  ScheduleDIDOperationOutcome,
  ScheduledDIDOperationDetail,
  SignedPrismDIDOperation
}
import zio.*
import io.iohk.atala.castor.core.model.ProtoModelHelper
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.prism.protos.{node_api, node_models}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeServiceStub
import io.iohk.atala.prism.protos.node_models.OperationOutput.{OperationMaybe, Result}

import scala.collection.immutable.{AbstractSeq, ArraySeq, LinearSeq}

trait DIDService {
  def createPublishedDID(operation: SignedPrismDIDOperation.Create): IO[DIDOperationError, ScheduleDIDOperationOutcome]
  def getScheduledDIDOperationDetail(
      operationId: Array[Byte]
  ): IO[DIDOperationError, Option[ScheduledDIDOperationDetail]]
}

object DIDServiceImpl {
  val layer: URLayer[NodeServiceStub & DIDOperationValidator, DIDService] =
    ZLayer.fromFunction(DIDServiceImpl(_, _))
}

private class DIDServiceImpl(didOpValidator: DIDOperationValidator, nodeClient: NodeServiceStub)
    extends DIDService,
      ProtoModelHelper {

  override def createPublishedDID(
      signedOperation: SignedPrismDIDOperation.Create
  ): IO[DIDOperationError, ScheduleDIDOperationOutcome] = {
    val operationRequest = node_api.ScheduleOperationsRequest(
      signedOperations = Seq(
        node_models.SignedAtalaOperation(
          signedWith = signedOperation.signedWithKey,
          signature = signedOperation.signature.toArray.toProto,
          operation = Some(signedOperation.operation.toAtalaOperation)
        )
      )
    )
    for {
      _ <- ZIO.fromEither(didOpValidator.validate(signedOperation.operation))
      operationOutput <- ZIO
        .fromFuture(_ => nodeClient.scheduleOperations(operationRequest))
        .mapBoth(DIDOperationError.DLTProxyError.apply, _.outputs.toList)
        .map {
          case output :: Nil => Right(output)
          case _ => Left(DIDOperationError.UnexpectedDLTResult("createDID operation result must have exactly 1 output"))
        }
        .absolve
      operationId <- ZIO.fromEither {
        operationOutput.operationMaybe match {
          case OperationMaybe.OperationId(id) => Right(id.toByteArray)
          case OperationMaybe.Empty =>
            Left(DIDOperationError.UnexpectedDLTResult("createDID operation result does not contain operation detail"))
          case OperationMaybe.Error(e) =>
            Left(DIDOperationError.UnexpectedDLTResult(s"createDID operation result was not successful: $e"))
        }
      }
      suffix <- ZIO.fromEither {
        operationOutput.result match {
          case Result.CreateDidOutput(createDIDOutput) => Right(createDIDOutput.didSuffix)
          case _ =>
            Left(
              DIDOperationError.UnexpectedDLTResult("createDID operation result must have a type of CreateDIDOutput")
            )
        }
      }
      did <- ZIO
        .fromTry(HexString.fromString(suffix))
        .mapError(_ =>
          DIDOperationError
            .UnexpectedDLTResult(s"createDID operation result must have suffix formatted in hex string: $suffix")
        )
        .map(suffix =>
          PrismDID
            .buildCanonical(suffix.toByteArray)
            .left
            .map(e =>
              DIDOperationError.UnexpectedDLTResult(s"createDID operation result must have a valid DID suffix: $e")
            )
        )
        .absolve
    } yield ScheduleDIDOperationOutcome(
      did = did,
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

}
