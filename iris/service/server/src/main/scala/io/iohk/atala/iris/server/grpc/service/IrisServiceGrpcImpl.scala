package io.iohk.atala.iris.server.grpc.service

import com.google.protobuf.ByteString
import io.iohk.atala.iris.core.service.PublishingService
import io.iohk.atala.iris.core.worker.PublishingScheduler
import io.iohk.atala.iris.proto.did_operations.{CreateDid, DocumentDefinition}
import io.iohk.atala.iris.proto.dlt.IrisOperation
import io.iohk.atala.iris.proto.service.*
import zio.*

import scala.concurrent.Future

class IrisServiceGrpcImpl(service: PublishingScheduler)(using runtime: Runtime[Any]) extends IrisServiceGrpc.IrisService {

  private val mockOperationId = ByteString.copyFrom("aaafff111".getBytes())
  private val mockOperation = IrisOperationInfo.Operation.CreateDid(
    CreateDid(
      initialUpdateCommitment = ByteString.copyFrom("a".getBytes()),
      initialRecoveryCommitment = ByteString.copyFrom("b".getBytes()),
      ledger = "https://atalaprism.io",
      document = Some(DocumentDefinition(publicKeys = Seq(), services = Seq()))
    ))

  override def scheduleOperation(request: IrisOperation): Future[IrisOperationOutcome] = Unsafe.unsafe { implicit unsafe =>
    runtime.unsafe.runToFuture(ZIO.succeed(IrisOperationOutcome(mockOperationId)))
  }

  override def getOperation(request: IrisOperationId): Future[IrisOperationInfo] = Unsafe.unsafe { implicit unsafe =>
    runtime.unsafe.runToFuture(ZIO.succeed(
      IrisOperationInfo(
        operationId = mockOperationId,
        operation = mockOperation
      )))
  }
}

object IrisServiceGrpcImpl {
  val layer: URLayer[PublishingScheduler, IrisServiceGrpc.IrisService] = ZLayer.fromZIO {
    for {
      rt <- ZIO.runtime[Any]
      svc <- ZIO.service[PublishingScheduler]
    } yield IrisServiceGrpcImpl(svc)(using rt)
  }
}
