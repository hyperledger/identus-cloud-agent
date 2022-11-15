package io.iohk.atala.castor.sql.model

import io.iohk.atala.castor.core.model.ProtoModelHelper
import io.iohk.atala.castor.core.model.did.ConfirmedPublishedDIDOperation
import io.iohk.atala.castor.sql.model.OperationType

import scala.util.Try

private[sql] trait SqlModelHelper extends ProtoModelHelper {

  // TODO: implement all operation types
  extension (row: ConfirmedPublishedDIDOperationRow) {
    def toDomain: Either[String, ConfirmedPublishedDIDOperation] = {
//      import OperationType.*
//      val errorOrParsedOperation = row.operationType match {
//        case CREATE =>
//          Try(iris_proto.did_operations.CreateDid.parseFrom(row.operationContent)).toEither.left
//            .map(ex => s"unable to parse CreateDid to domain model: ${ex.getMessage}")
//            .flatMap(_.toDomain)
//        case UPDATE     => Left("UPDATE operation model conversion is not yet implemented")
//        case RECOVER    => Left("RECOVER operation model conversion is not yet implemented")
//        case DEACTIVATE => Left("DEACTIVATE operation model conversion is not yet implemented")
//      }
//
//      errorOrParsedOperation.map { operation =>
//        ConfirmedPublishedDIDOperation(
//          operation = operation,
//          anchoredAt = row.anchoredAt,
//          blockNumber = row.blockNumber,
//          blockIndex = row.blockIndex
//        )
//      }
      ???
    }
  }

}
