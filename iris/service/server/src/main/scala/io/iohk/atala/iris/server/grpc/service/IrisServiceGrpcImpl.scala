package io.iohk.atala.iris.server.grpc.service

import com.google.protobuf.ByteString
import io.grpc.stub.StreamObserver
import io.iohk.atala.iris.core.model.ledger.TransactionId
import io.iohk.atala.iris.core.repository.ROIrisBatchesRepository
import io.iohk.atala.iris.core.service.PublishingService
import io.iohk.atala.iris.core.worker.PublishingScheduler
import io.iohk.atala.iris.proto.did_operations.{CreateDid, DocumentDefinition}
import io.iohk.atala.iris.proto.{dlt as proto, service as proto_service}
import com.google.protobuf.timestamp as proto_google
import io.iohk.atala.iris.proto.service.*
import zio.*
import zio.stream.*

import scala.concurrent.Future

type Stream[A] = ZStream[Any, Throwable, A]

class IrisServiceGrpcImpl(service: PublishingScheduler, batchRepo: ROIrisBatchesRepository[Stream])(using
    runtime: Runtime[Any]
) extends IrisServiceGrpc.IrisService {

  private val mockOperationId = ByteString.copyFrom("aaafff111".getBytes())
  private val mockOperation = IrisOperationInfo.Operation.CreateDid(
    CreateDid(
      initialUpdateCommitment = ByteString.copyFrom("a".getBytes()),
      initialRecoveryCommitment = ByteString.copyFrom("b".getBytes()),
      ledger = "https://atalaprism.io",
      document = Some(DocumentDefinition(publicKeys = Seq(), services = Seq()))
    )
  )

  override def scheduleOperation(request: proto.IrisOperation): Future[IrisOperationOutcome] = Unsafe.unsafe {
    implicit unsafe =>
      runtime.unsafe.runToFuture(ZIO.succeed(IrisOperationOutcome(mockOperationId)))
  }

  override def getOperation(request: IrisOperationId): Future[IrisOperationInfo] = Unsafe.unsafe { implicit unsafe =>
    runtime.unsafe.runToFuture(
      ZIO.succeed(
        IrisOperationInfo(
          operationId = mockOperationId,
          operation = mockOperation
        )
      )
    )
  }

  override def getIrisBatchStream(
      request: IrisBatchRequest,
      responseObserver: StreamObserver[ConfirmedIrisBatch]
  ): Unit = {
    Unsafe.unsafe { implicit unsafe =>
      val txIdHex = request.lastSeenTransactionId
      runtime.unsafe
        .run {
          for {
            txId <-
              if (txIdHex.isEmpty) ZIO.succeed(None)
              else { ZIO.fromOption(TransactionId.from(txIdHex)).map(Some(_)) }
            _ <- batchRepo
              .getIrisBatchesStream(txId)
              .foreach { b =>
                ZIO.succeedBlocking {
                  responseObserver.onNext(
                    proto_service
                      .ConfirmedIrisBatch(
                        blockLevel = b.blockLevel,
                        blockTimestamp =
                          Some(proto_google.Timestamp(b.blockTimestamp.getEpochSecond, b.blockTimestamp.getNano)),
                        transactionId = b.transactionId.toString,
                        batch = Some(b.batch)
                      )
                  )
                }
              }
              .onError { cause =>
                cause.failureOption.fold(ZIO.unit) { e =>
                  ZIO.succeedBlocking {
                    responseObserver.onError(e)
                  }
                }
              }
          } yield ()
        }
        .getOrThrowFiberFailure()
    }
  }
}

object IrisServiceGrpcImpl {
  val layer: URLayer[PublishingScheduler & ROIrisBatchesRepository[Stream], IrisServiceGrpc.IrisService] =
    ZLayer.fromZIO {
      for {
        rt <- ZIO.runtime[Any]
        svc <- ZIO.service[PublishingScheduler]
        repo <- ZIO.service[ROIrisBatchesRepository[Stream]]
      } yield IrisServiceGrpcImpl(svc, repo)(using rt)
    }
}
