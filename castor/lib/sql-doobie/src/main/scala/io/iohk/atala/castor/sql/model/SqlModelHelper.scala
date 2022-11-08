package io.iohk.atala.castor.sql.model

import io.iohk.atala.castor.core.model.ProtoModelHelper
import io.iohk.atala.castor.core.model.did.{ConfirmedPublishedDIDOperation, PublishedDIDOperation}
import io.iohk.atala.castor.sql.model.OperationType
import io.iohk.atala.iris.proto as iris_proto

import scala.util.Try

private[sql] trait SqlModelHelper extends ProtoModelHelper {

  // TODO: implement all operation types
  extension (row: ConfirmedPublishedDIDOperationRow) {
    def toDomain: Either[String, ConfirmedPublishedDIDOperation] = {
      import OperationType.*
      val errorOrParsedOperation: Either[String, PublishedDIDOperation] = row.operationType match {
        case CREATE =>
          Try(iris_proto.did_operations.CreateDid.parseFrom(row.operationContent)).toEither.left
            .map(e => s"unable to parse CreateDID to domain model: ${e.getMessage}")
            .flatMap(_.toDomain)
        case UPDATE =>
          Try(iris_proto.did_operations.UpdateDid.parseFrom(row.operationContent)).toEither.left
            .map(e => s"unable to parse UpdateDID to domain model: ${e.getMessage}")
            .flatMap(_.toDomain)
        case RECOVER    => Left("RECOVER operation model conversion is not yet implemented")
        case DEACTIVATE => Left("DEACTIVATE operation model conversion is not yet implemented")
      }

      errorOrParsedOperation.map { operation =>
        ConfirmedPublishedDIDOperation(
          operation = operation,
          anchoredAt = row.anchoredAt,
          blockNumber = row.blockNumber,
          blockIndex = row.blockIndex
        )
      }
    }
  }

}
