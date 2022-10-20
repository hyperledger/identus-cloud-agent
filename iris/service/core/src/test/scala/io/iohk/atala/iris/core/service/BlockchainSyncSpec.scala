package io.iohk.atala.iris.core.service

import io.iohk.atala.iris.core.mock.{
  InMemoryIrisBatchesRepository,
  InMemoryKeyValueRepository,
  DummyDbRepositoryTransactor
}
import io.iohk.atala.iris.core.model.ledger.{Funds, Ledger, TransactionId}
import io.iohk.atala.iris.core.testutils.PublishThenAdjust
import io.iohk.atala.iris.core.testutils.PublishThenAdjust.*
import io.iohk.atala.iris.core.testutils.RandomUtils.*
import zio.*
import zio.stream.*
import zio.test.*
import zio.interop.catz.*

object BlockchainSyncSpec extends ZIOSpecDefault {
  val blockEvery = 10.seconds
  val inmemoryDefaultConfig = InmemoryUnderlyingLedgerService.Config(blockEvery, Funds(1000), Funds(1), Ledger.Mainnet)
  val inmemoryLedgerLayer = InmemoryUnderlyingLedgerService.layer(inmemoryDefaultConfig)

  val keyValueRepoLayer = InMemoryKeyValueRepository.layer

  val blockchainStreamerConfig1BlockConfirm = BlocksStreamer.Config(Ledger.Mainnet, 0, 1, blockEvery)
  val blockchainStreamer1Layer: TaskLayer[BlocksStreamer] =
    (inmemoryLedgerLayer ++ keyValueRepoLayer) >>> BlocksStreamer.layer(blockchainStreamerConfig1BlockConfirm)

  val blockchainStreamerConfig3BlocksConfirm: BlocksStreamer.Config =
    BlocksStreamer.Config(Ledger.Mainnet, 0, 3, blockEvery)
  val blockchainStreamer3Layer: TaskLayer[BlocksStreamer] =
    (inmemoryLedgerLayer ++ keyValueRepoLayer) >>> BlocksStreamer.layer(blockchainStreamerConfig3BlocksConfirm)

  type StreamZIO[A] = Stream[Throwable, A]
  val irisBatchesRepoLayer = InMemoryIrisBatchesRepository.layer
  val blockchainSaver: TaskLayer[BlocksSaveSinker] =
    (keyValueRepoLayer ++ irisBatchesRepoLayer ++ DummyDbRepositoryTransactor.layer) >>> BlocksSaveSinker
      .layer[Task, StreamZIO]

  override def spec = suite("BlockchainSyncSpec")(
    test("Sync up 1 block with 4 transactions") {
      val testCase =
        for {
          blocksSource <- ZIO.service[BlocksStreamer]
          blocksSink <- ZIO.service[BlocksSaveSinker]
          _ <- blocksSource.blocksStream.run(blocksSink.sink).fork
          op <- ZIO.replicateZIO(4)(genOperation()).map(_.toList)
          srvc <- ZIO.service[InmemoryUnderlyingLedgerService]
          scenario = List(
            Seq(op(0)) >> 1.seconds,
            Seq(op(1)) >> 1.seconds,
            Seq(op(2)) >> 0.seconds,
            Seq(op(3)) >> 20.seconds
          )
          _ <- PublishThenAdjust.foreachZIO(srvc)(scenario)
          irisBatchesRepo <- ZIO.service[InMemoryIrisBatchesRepository]
          irisBatches <- irisBatchesRepo.getConfirmedBatches
          expected = Vector(
            "c4556a3d133b0a184a01baa9f3ea76a8fef2a06e66dec0907038997b2d7588de",
            "8a89c3c1bbc39b5e5eb0db0ed8b12d876ec89f45a7dfeaaa7c24e39ed974aab1",
            "0872cced55cab747ae0a3d463e5713e5cb9225617af04d7243b21d9d82751986",
            "29798b9678930bc07c097adffaf3e13ae044af64d2b950af0414c231e3a06b8a"
          ).map(TransactionId.from(_).get)
        } yield assertTrue(irisBatches.map(_.transactionId) == expected)
      testCase.provideLayer(inmemoryLedgerLayer ++ blockchainStreamer1Layer ++ blockchainSaver ++ irisBatchesRepoLayer)
    },
    test("Sync up 1 block with 2 transaction") {
      val testCase =
        for {
          blocksSource <- ZIO.service[BlocksStreamer]
          blocksSink <- ZIO.service[BlocksSaveSinker]
          _ <- blocksSource.blocksStream.run(blocksSink.sink).fork
          op <- ZIO.replicateZIO(4)(genOperation()).map(_.toList)
          srvc <- ZIO.service[InmemoryUnderlyingLedgerService]
          scenario = List(
            Seq(op(0), op(1)) >> 1.seconds,
            Seq(op(2), op(3)) >> 20.seconds
          )
          _ <- PublishThenAdjust.foreachZIO(srvc)(scenario)
          irisBatchesRepo <- ZIO.service[InMemoryIrisBatchesRepository]
          irisBatches <- irisBatchesRepo.getConfirmedBatches
          expected = Vector(
            "1c6fd29ae378a773b2e3957be37ead077aa98d7041a3d4bb6533eb0a95e4058c",
            "919e893246ffd0543d9005d9fdea6e2b26b85b150eb953fc4ba368097546d347"
          ).map(TransactionId.from(_).get)
        } yield assertTrue(irisBatches.map(_.transactionId) == expected)
      testCase.provideLayer(
        inmemoryLedgerLayer ++ blockchainStreamer1Layer ++ blockchainSaver ++ irisBatchesRepoLayer
      )
    },
    test("Block confirmation is 3") {
      val testCase =
        for {
          blocksSource <- ZIO.service[BlocksStreamer]
          blocksSink <- ZIO.service[BlocksSaveSinker]
          _ <- blocksSource.blocksStream.run(blocksSink.sink).fork
          op <- ZIO.replicateZIO(6)(genOperation()).map(_.toList)
          srvc <- ZIO.service[InmemoryUnderlyingLedgerService]
          scenario = List(
            Seq(op(0)) >> 1.seconds,
            Seq(op(1), op(2)) >> blockEvery,
            Seq(op(3)) >> blockEvery,
            Seq(op(4)) >> blockEvery,
            Seq(op(5)) >> blockEvery,
          )
          _ <- PublishThenAdjust.foreachZIO(srvc)(scenario)
          irisBatchesRepo <- ZIO.service[InMemoryIrisBatchesRepository]
          expected = Vector(
            "c4556a3d133b0a184a01baa9f3ea76a8fef2a06e66dec0907038997b2d7588de",
            "de69eda103be2676872937a0622cb9d831939d595fb9f80fba5da36cfe28d174"
          ).map(TransactionId.from(_).get)
          irisBatches <- irisBatchesRepo.getConfirmedBatches
        } yield assertTrue(irisBatches.map(_.transactionId) == expected)
      testCase.provideLayer(
        inmemoryLedgerLayer ++ blockchainStreamer3Layer ++ blockchainSaver ++ irisBatchesRepoLayer
      )
    },
  )
}
