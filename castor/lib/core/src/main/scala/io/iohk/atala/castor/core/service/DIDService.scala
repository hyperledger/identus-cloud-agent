package io.iohk.atala.castor.core.service

import io.iohk.atala.castor.core.model.did.{DIDDocument, PublishedDIDOperation}
import zio.*
import io.iohk.atala.castor.core.model.ProtoModelHelper
import io.iohk.atala.castor.core.model.error.DIDOperationError
import io.iohk.atala.castor.core.util.DIDOperationValidator
import io.iohk.atala.iris.proto.service.IrisServiceGrpc.IrisServiceStub
import io.iohk.atala.prism.crypto.Sha256
import io.iohk.atala.shared.models.HexStrings.HexString

trait DIDService {
  def createPublishedDID(operation: PublishedDIDOperation.Create): IO[DIDOperationError, PublishedDIDOperation.Create]
}

object MockDIDService {
  val layer: ULayer[DIDService] = ZLayer.succeed {
    new DIDService {
      def createPublishedDID(
          operation: PublishedDIDOperation.Create
      ): IO[DIDOperationError, PublishedDIDOperation.Create] =
        ZIO.fail(DIDOperationError.InvalidArgument("mocked error"))
    }
  }
}

object DIDServiceImpl {
  val layer: URLayer[IrisServiceStub & DIDOperationValidator, DIDService] = ZLayer.fromFunction(DIDServiceImpl(_, _))
}

private class DIDServiceImpl(irisClient: IrisServiceStub, operationValidator: DIDOperationValidator)
    extends DIDService,
      ProtoModelHelper {

  // TODO:
  // 1. generate DID identifier from operation
  // 2. check if DID already exists
  // 3. persist state
  override def createPublishedDID(
      operation: PublishedDIDOperation.Create
  ): IO[DIDOperationError, PublishedDIDOperation.Create] = {
    for {
      _ <- ZIO.fromEither(operationValidator.validate(operation))
      _ <- ZIO
        .fromFuture(_ => irisClient.scheduleOperation(operation.toProto))
        .mapError(DIDOperationError.DLTProxyError.apply)
    } yield operation
  }

}
