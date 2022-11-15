package io.iohk.atala.castor.core.service

import io.iohk.atala.castor.core.model.did.{PrismDID, PublishedDIDOperationOutcome, SignedPrismDIDOperation}
import zio.*
import io.iohk.atala.castor.core.model.ProtoModelHelper
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.shared.models.HexStrings.*
import io.iohk.atala.prism.protos.{node_api, node_models}
import io.iohk.atala.prism.protos.node_api.NodeServiceGrpc.NodeServiceStub
import io.iohk.atala.prism.protos.node_models.OperationOutput.{OperationMaybe, Result}

import scala.collection.immutable.{AbstractSeq, ArraySeq, LinearSeq}

trait DIDService {
  def createPublishedDID(operation: SignedPrismDIDOperation.Create): IO[DIDOperationError, PublishedDIDOperationOutcome]
}

object DIDServiceImpl {
  val layer: URLayer[NodeServiceStub, DIDService] =
    ZLayer.fromFunction(DIDServiceImpl(_))
}

private class DIDServiceImpl(nodeClient: NodeServiceStub) extends DIDService, ProtoModelHelper {

  override def createPublishedDID(
      signedOperation: SignedPrismDIDOperation.Create
  ): IO[DIDOperationError, PublishedDIDOperationOutcome] = {
    val operationRequest = node_api.ScheduleOperationsRequest(
      signedOperations = Seq(
        node_models.SignedAtalaOperation(
          signedWith = signedOperation.signedWithKey,
          signature = signedOperation.signature.toArray.toProto,
          operation = Some(node_models.AtalaOperation(signedOperation.operation.toProto))
        )
      )
    )
    for {
      operationOutput <- ZIO
        .fromFuture(_ => nodeClient.scheduleOperations(operationRequest))
        .mapBoth(DIDOperationError.DLTProxyError.apply, _.outputs)
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
        .mapBoth(
          _ =>
            DIDOperationError
              .UnexpectedDLTResult(s"createDID operation result must have suffix formatted in hex string: $suffix"),
          suffix => PrismDID.buildCanonical(suffix.toByteArray)
        )
    } yield PublishedDIDOperationOutcome(
      did = did,
      operation = signedOperation.operation,
      operationId = ArraySeq.from(operationId)
    )
  }

}
