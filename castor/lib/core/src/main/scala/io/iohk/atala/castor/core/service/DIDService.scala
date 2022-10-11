package io.iohk.atala.castor.core.service

import io.iohk.atala.castor.core.model.did.{DIDDocument, PublishedDIDOperation, PublishedDIDOperationOutcome}
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
    val createDIDProto = operation.toProto
    val irisOpProto = iris_proto.dlt.IrisOperation(
      operation = iris_proto.dlt.IrisOperation.Operation.CreateDid(createDIDProto)
    )
    val didSuffix = HexString.fromStringUnsafe(Sha256.compute(createDIDProto.toByteArray).getHexValue)
    for {
      _ <- ZIO.fromEither(operationValidator.validate(operation))
      confirmedOps <- didOpRepo
        .getConfirmedPublishedDIDOperations(didSuffix)
        .mapError(DIDOperationError.InternalErrorDB.apply)
      _ <- confirmedOps
        .map(_.operation)
        .collectFirst { case op: PublishedDIDOperation.Create => op }
        .fold(ZIO.unit)(_ =>
          ZIO.fail(
            DIDOperationError
              .InvalidPrecondition(s"PublishedDID with suffix $didSuffix has already been created and confirmed")
          )
        )
      operationId <- ZIO
        .fromFuture(_ => irisClient.scheduleOperation(irisOpProto))
        .mapError(DIDOperationError.DLTProxyError.apply)
    } yield operationId.toDomain
  }

}
