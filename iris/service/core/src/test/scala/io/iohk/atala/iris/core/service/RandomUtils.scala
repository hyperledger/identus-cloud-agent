package io.iohk.atala.iris.core.service

import com.google.protobuf.ByteString
import io.iohk.atala.iris.proto.did_operations.{CreateDid, DocumentDefinition, UpdateDid}
import io.iohk.atala.iris.proto.dlt as proto
import zio.{Random, UIO}

object RandomUtils {

  private def nextBytes(length: Int): UIO[ByteString] =
    Random.nextBytes(length).map(x => ByteString.copyFrom(x.toArray))

  def genCreateOperation(): UIO[proto.IrisOperation] =
    for {
      updComm <- nextBytes(20)
      recComm <- nextBytes(20)
    } yield
      proto.IrisOperation(proto.IrisOperation.Operation.CreateDid(
        CreateDid(
          initialUpdateCommitment = updComm,
          initialRecoveryCommitment = recComm,
          storage = "mainnet",
          document = Some(DocumentDefinition(publicKeys = Seq(), services = Seq()))
        )
      )
      )

  def genUpdateOperation(): UIO[proto.IrisOperation] =
    for {
      didSuff <- Random.nextString(10)
      updKey <- nextBytes(20)
      prevVers <- nextBytes(20)
      forwUpdComm <- nextBytes(20)
      sig <- nextBytes(20)
    } yield
      proto.IrisOperation(proto.IrisOperation.Operation.UpdateDid(
        UpdateDid(
          did = "did:prism:" + didSuff,
          revealedUpdateKey = updKey,
          previousVersion = prevVers,
          forwardUpdateCommitment = forwUpdComm,
          patches = Seq(),
          signature = sig
        )
      )
      )

  def genOperation(): UIO[proto.IrisOperation] =
    for {
      op <- Random.nextBoolean
      res <- if (op) genCreateOperation()
      else genUpdateOperation()
    } yield res
}
