package io.iohk.atala.iris.core.service

import com.google.protobuf.ByteString
import io.iohk.atala.iris.core.model.ledger.TransactionStatus.{InLedger, InMempool}
import io.iohk.atala.iris.core.model.ledger.{Funds, TransactionDetails}
import io.iohk.atala.iris.core.service.InmemoryUnderlyingLedgerService.{CardanoBlock, CardanoTransaction}
import io.iohk.atala.iris.core.service.RandomUtils.*
import io.iohk.atala.iris.proto.did_operations.{CreateDid, DocumentDefinition, UpdateDid}
import io.iohk.atala.iris.proto.dlt as proto
import zio.*
import zio.test.*
import zio.test.Assertion.*

object InmemoryUnderlyingLedgerServiceSpec extends ZIOSpecDefault {
  val defaultConfig = InmemoryUnderlyingLedgerService.Config(10.seconds, Funds(1000), Funds(1))
  val inmemoryLedger = InmemoryUnderlyingLedgerService.layer(defaultConfig)

  case class PublishThenAdjust(operations: Seq[proto.IrisOperation], adjust: Duration)

  object PublishThenAdjust {
    implicit class Then(operations: Seq[proto.IrisOperation]) {
      def >>(adj: Duration): PublishThenAdjust = PublishThenAdjust(operations, adj)
    }

    def foreachZIO[R](srv: InmemoryUnderlyingLedgerService)(xs: Iterable[PublishThenAdjust]): ZIO[R, Any, Unit] =
      ZIO.foreachDiscard[R, Any, PublishThenAdjust](xs) {
        case PublishThenAdjust(ops, adj) =>
          srv.publish(ops).flatMap(_ => TestClock.adjust(adj))
      }
  }

  import PublishThenAdjust.Then

  def spec = suite("InmemoryUnderlyingLedgerServiceSpec")(
    suite("Background worker")(
      test("All the operations in the one block within 4 different transactions") {
        val testCase =
          for {
            op <- ZIO.replicateZIO(4)(genOperation()).map(_.toList)
            srvc <- ZIO.service[InmemoryUnderlyingLedgerService]
            scenario = List(
              Seq(op(0)) >> 1.seconds,
              Seq(op(1)) >> 1.seconds,
              Seq(op(2)) >> 0.seconds,
              Seq(op(3)) >> 20.seconds
            )
            _ <- PublishThenAdjust.foreachZIO(srvc)(scenario)
            mempool <- srvc.getMempool
            blocks <- srvc.getBlocks
          } yield
            assertTrue(
              mempool == List.empty &&
                blocks == List(
                  CardanoBlock(List()),
                  CardanoBlock(List(
                    CardanoTransaction(Seq(op(0))),
                    CardanoTransaction(Seq(op(1))),
                    CardanoTransaction(Seq(op(2))),
                    CardanoTransaction(Seq(op(3)))
                  )),
                  CardanoBlock(List())
                )
            )
        testCase.provideLayer(inmemoryLedger)
      },

      test("Operations distributed between 2 blocks") {
        val testCase =
          for {
            op <- ZIO.replicateZIO(4)(genOperation()).map(_.toList)
            srvc <- ZIO.service[InmemoryUnderlyingLedgerService]
            scenario = List(
              Seq(op(0)) >> 1.seconds,
              Seq(op(1)) >> 10.seconds,

              Seq(op(2)) >> 0.seconds,
              Seq(op(3)) >> 10.seconds
            )
            _ <- PublishThenAdjust.foreachZIO(srvc)(scenario)
            mempool <- srvc.getMempool
            blocks <- srvc.getBlocks
          } yield
            assertTrue(
              mempool == List.empty &&
                blocks == List(
                  CardanoBlock(List()),
                  CardanoBlock(List(
                    CardanoTransaction(Seq(op(0))),
                    CardanoTransaction(Seq(op(1))),
                  )),
                  CardanoBlock(List(
                    CardanoTransaction(Seq(op(2))),
                    CardanoTransaction(Seq(op(3))),
                  )),
                )
            )
        testCase.provideLayer(inmemoryLedger)
      }
    ),

    suite("getTransactionDetails")(
      test("Find unconfirmed transaction") {
        val testCase =
          for {
            op <- ZIO.replicateZIO(5)(genOperation()).map(_.toList)
            srvc <- ZIO.service[InmemoryUnderlyingLedgerService]
            scenario = List(
              Seq(op(0)) >> 1.seconds,
              Seq(op(1)) >> 10.seconds,

              Seq(op(2), op(3)) >> 0.seconds,
              Seq(op(4)) >> 2.seconds
            )
            _ <- PublishThenAdjust.foreachZIO(srvc)(scenario)
            targetTx = CardanoTransaction(Seq(op(2), op(3)))
            txDetails <- srvc.getTransactionDetails(targetTx.transactionId)
          } yield
            assertTrue(txDetails == TransactionDetails(targetTx.transactionId, InMempool))
        testCase.provideLayer(inmemoryLedger)
      },

      test("Find confirmed transaction") {
        val testCase =
          for {
            op <- ZIO.replicateZIO(5)(genOperation()).map(_.toList)
            srvc <- ZIO.service[InmemoryUnderlyingLedgerService]
            scenario = List(
              Seq(op(0)) >> 11.seconds,
              Seq(op(1)) >> 11.seconds,
              Seq(op(2), op(3)) >> 0.seconds,
              Seq(op(4)) >> 12.seconds
            )
            _ <- PublishThenAdjust.foreachZIO(srvc)(scenario)
            targetTx = CardanoTransaction(Seq(op(2), op(3)))
            txDetails <- srvc.getTransactionDetails(targetTx.transactionId)
          } yield
            assertTrue(txDetails == TransactionDetails(targetTx.transactionId, InLedger))
        testCase.provideLayer(inmemoryLedger)
      },

      test("Find unknown transaction") {
        val testCase =
          for {
            op <- ZIO.replicateZIO(5)(genOperation()).map(_.toList)
            srvc <- ZIO.service[InmemoryUnderlyingLedgerService]
            scenario = List(
              Seq(op(0)) >> 11.seconds,
              Seq(op(1)) >> 11.seconds,
              Seq(op(2), op(3)) >> 0.seconds,
              Seq(op(4)) >> 12.seconds
            )
            _ <- PublishThenAdjust.foreachZIO(srvc)(scenario)
            targetTx = CardanoTransaction(Seq(op(1), op(2)))
            testResult <- assertZIO(srvc.getTransactionDetails(targetTx.transactionId).exit) {
              fails(equalTo(LedgerError(s"Couldn't find tx ${targetTx.transactionId}")))
            }
          } yield testResult
        testCase.provideLayer(inmemoryLedger)
      }
    ),

    suite("deleteTransaction")(
      test("Delete transaction from mempool") {
        val testCase =
          for {
            op <- ZIO.replicateZIO(5)(genOperation()).map(_.toList)
            srvc <- ZIO.service[InmemoryUnderlyingLedgerService]
            scenario = List(
              Seq(op(0)) >> 1.seconds,
              Seq(op(1)) >> 10.seconds,

              Seq(op(2), op(3)) >> 0.seconds,
              Seq(op(4)) >> 2.seconds
            )
            _ <- PublishThenAdjust.foreachZIO(srvc)(scenario)
            targetTx = CardanoTransaction(Seq(op(2), op(3)))
            _ <- srvc.deleteTransaction(targetTx.transactionId)
            mempool <- srvc.getMempool
          } yield
              assertTrue(mempool ==  List(CardanoTransaction(Seq(op(4)))))
        testCase.provideLayer(inmemoryLedger)
      },

      test("Delete confirmed transaction") {
        val testCase =
          for {
            op <- ZIO.replicateZIO(5)(genOperation()).map(_.toList)
            srvc <- ZIO.service[InmemoryUnderlyingLedgerService]
            scenario = List(
              Seq(op(0)) >> 1.seconds,
              Seq(op(1)) >> 10.seconds,

              Seq(op(2), op(3)) >> 0.seconds,
              Seq(op(4)) >> 2.seconds
            )
            _ <- PublishThenAdjust.foreachZIO(srvc)(scenario)
            targetTx = CardanoTransaction(Seq(op(1)))
            testResult <-
              assertZIO(srvc.deleteTransaction(targetTx.transactionId).exit) {
                fails(
                  equalTo(LedgerError(s"Transaction ${targetTx.transactionId} not found in the mempool")))
              }
          } yield testResult
        testCase.provideLayer(inmemoryLedger)
      }
    )
  )
}
