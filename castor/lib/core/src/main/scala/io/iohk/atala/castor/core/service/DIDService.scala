package io.iohk.atala.castor.core.service

import io.iohk.atala.castor.core.model.did.{
  DIDDocument,
  PrismDIDV1,
  PublishedDIDOperation,
  PublishedDIDOperationOutcome
}
import zio.*
import io.iohk.atala.castor.core.model.ProtoModelHelper
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.repository.DIDOperationRepository
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.iris.proto.service.IrisServiceGrpc.IrisServiceStub
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.shared.models.HexStrings.HexString
import io.iohk.atala.iris.proto as iris_proto

trait DIDService {
  def createPublishedDID(operation: PublishedDIDOperation.Create): IO[DIDOperationError, PublishedDIDOperationOutcome]
}

object MockDIDService {
  val layer: ULayer[DIDService] = ZLayer.succeed {
    new DIDService {
      def createPublishedDID(
          operation: PublishedDIDOperation.Create
      ): IO[DIDOperationError, PublishedDIDOperationOutcome] =
        ZIO.fail(DIDOperationError.InvalidArgument("mocked error"))
    }
  }
}

object DIDServiceImpl {
  val layer: URLayer[IrisServiceStub & DIDOperationValidator & DIDOperationRepository[Task], DIDService] =
    ZLayer.fromFunction(DIDServiceImpl(_, _, _))
}

private class DIDServiceImpl(
    irisClient: IrisServiceStub,
    operationValidator: DIDOperationValidator,
    didOpRepo: DIDOperationRepository[Task]
) extends DIDService,
      ProtoModelHelper {

  override def createPublishedDID(
      operation: PublishedDIDOperation.Create
  ): IO[DIDOperationError, PublishedDIDOperationOutcome] = {
    val prismDID = PrismDIDV1.fromCreateOperation(operation)
    val irisOpProto = iris_proto.dlt.IrisOperation(
      operation = iris_proto.dlt.IrisOperation.Operation.CreateDid(operation.toProto)
    )
    for {
      _ <- ZIO.fromEither(operationValidator.validate(operation))
      confirmedOps <- didOpRepo
        .getConfirmedPublishedDIDOperations(prismDID)
        .mapError(DIDOperationError.InternalErrorDB.apply)
      _ <- confirmedOps
        .map(_.operation)
        .collectFirst { case op: PublishedDIDOperation.Create => op }
        .fold(ZIO.unit)(_ =>
          ZIO.fail(
            DIDOperationError
              .InvalidPrecondition(s"PublishedDID with suffix ${prismDID.did} has already been created and confirmed")
          )
        )
      irisOutcome <- ZIO
        .fromFuture(_ => irisClient.scheduleOperation(irisOpProto))
        .mapError(DIDOperationError.DLTProxyError.apply)
      _ <- Console.printLine(s"creating ${prismDID.did}").ignore
    } yield PublishedDIDOperationOutcome(
      did = prismDID,
      operation = operation,
      operationId = HexString.fromByteArray(irisOutcome.operationId.toByteArray)
    )
  }

}
