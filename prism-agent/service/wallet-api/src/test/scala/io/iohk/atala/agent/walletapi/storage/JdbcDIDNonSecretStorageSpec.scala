package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.agent.walletapi.model.PublicationState
import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.agent.walletapi.service.WalletManagementServiceImpl
import io.iohk.atala.agent.walletapi.sql.JdbcDIDNonSecretStorage
import io.iohk.atala.agent.walletapi.sql.JdbcWalletNonSecretStorage
import io.iohk.atala.agent.walletapi.sql.JdbcWalletSecretStorage
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.castor.core.model.did.PrismDIDOperation
import io.iohk.atala.castor.core.model.did.ScheduledDIDOperationStatus
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.test.container.{DBTestUtils, PostgresTestContainerSupport}
import org.postgresql.util.PSQLException
import scala.collection.compat.immutable.ArraySeq
import zio.*
import zio.test.*
import zio.test.Assertion.*

object JdbcDIDNonSecretStorageSpec
    extends ZIOSpecDefault,
      StorageSpecHelper,
      PostgresTestContainerSupport,
      ApolloSpecHelper {

  private def insertDIDStateWithDelay(operation: Seq[PrismDIDOperation.Create], delay: zio.Duration) =
    ZIO.foreach(operation.zipWithIndex) { case (op, idx) =>
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        _ <- storage.insertManagedDID(
          op.did,
          ManagedDIDState(op, idx, PublicationState.Created()),
          Map.empty
        )
        _ <- TestClock.adjust(delay)
      } yield ()
    }

  override def spec = {
    val testSuite =
      suite("JdbcDIDNonSecretStorageSpec")(
        listDIDStateSpec,
        getDIDStateSpec,
        listDIDLineageSpec,
        setDIDLineageStatusSpec
      ).globalWallet @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)

    testSuite
      .provide(
        JdbcDIDNonSecretStorage.layer,
        JdbcWalletNonSecretStorage.layer,
        JdbcWalletSecretStorage.layer,
        WalletManagementServiceImpl.layer,
        transactorLayer,
        pgContainerLayer,
        apolloLayer,
      )
  }

  private val listDIDStateSpec = suite("listManagedDIDState")(
    test("initialize with empty list") {
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        resultsWithCount <- storage.listManagedDID(None, None)
        (results, count) = resultsWithCount
      } yield assert(results)(isEmpty) && assert(count)(isZero)
    },
    test("list stored dids") {
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        did1 = PrismDID.buildCanonicalFromSuffix("0" * 64).toOption.get
        did2 = PrismDID.buildCanonicalFromSuffix("1" * 64).toOption.get
        createOperation1 <- generateCreateOperation(Seq("key-1")).map(_._1)
        createOperation2 <- generateCreateOperation(Seq("key-1")).map(_._1)
        _ <- storage.insertManagedDID(
          did1,
          ManagedDIDState(createOperation1, 0, PublicationState.Created()),
          Map.empty
        )
        _ <- storage.insertManagedDID(
          did2,
          ManagedDIDState(createOperation2, 1, PublicationState.Created()),
          Map.empty
        )
        states <- storage.listManagedDID(None, None).map(_._1)
      } yield assert(states.map(_._1))(hasSameElements(Seq(did1, did2)))
    },
    test("list stored dids and return correct item count when using offset and limit") {
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        operations <- ZIO.foreach(1 to 50)(_ => generateCreateOperation(Seq("key-1")).map(_._1))
        _ <- insertDIDStateWithDelay(operations, 100.millis)
        resultsWithCount <- storage.listManagedDID(offset = Some(20), limit = Some(10))
        (results, count) = resultsWithCount
        dids = results.map(_._1.asCanonical)
      } yield assert(count)(equalTo(50)) && assert(dids)(hasSameElements(operations.drop(20).take(10).map(_.did)))
    },
    test("list stored dids and return correct item count when using offset only") {
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        operations <- ZIO.foreach(1 to 50)(_ => generateCreateOperation(Seq("key-1")).map(_._1))
        _ <- insertDIDStateWithDelay(operations, 100.millis)
        resultsWithCount <- storage.listManagedDID(offset = Some(20), limit = None)
        (results, count) = resultsWithCount
        dids = results.map(_._1)
      } yield assert(count)(equalTo(50)) && assert(dids)(hasSameElements(operations.drop(20).map(_.did)))
    },
    test("list stored dids and return correct item count when using limit only") {
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        operations <- ZIO.foreach(1 to 50)(_ => generateCreateOperation(Seq("key-1")).map(_._1))
        _ <- insertDIDStateWithDelay(operations, 100.millis)
        resultsWithCount <- storage.listManagedDID(offset = None, limit = Some(10))
        (results, count) = resultsWithCount
        dids = results.map(_._1)
      } yield assert(count)(equalTo(50)) && assert(dids)(hasSameElements(operations.take(10).map(_.did)))
    },
    test("return empty list when limit is zero") {
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        operations <- ZIO.foreach(1 to 50)(_ => generateCreateOperation(Seq("key-1")).map(_._1))
        _ <- insertDIDStateWithDelay(operations, 100.millis)
        resultsWithCount <- storage.listManagedDID(offset = None, limit = Some(0))
        (results, count) = resultsWithCount
        dids = results.map(_._1.asCanonical)
      } yield assert(count)(equalTo(50)) && assert(dids)(isEmpty)
    },
    test("fail when limit is negative") {
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        operations <- ZIO.foreach(1 to 50)(_ => generateCreateOperation(Seq("key-1")).map(_._1))
        _ <- insertDIDStateWithDelay(operations, 100.millis)
        exit <- storage.listManagedDID(offset = None, limit = Some(-1)).exit
      } yield assert(exit)(
        fails(
          isSubtype[PSQLException](hasField("getMessage", _.getMessage(), containsString("LIMIT must not be negative")))
        )
      )
    },
    test("fail when offset is negative") {
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        operations <- ZIO.foreach(1 to 50)(_ => generateCreateOperation(Seq("key-1")).map(_._1))
        _ <- insertDIDStateWithDelay(operations, 100.millis)
        exit <- storage.listManagedDID(offset = Some(-1), limit = None).exit
      } yield assert(exit)(
        fails(
          isSubtype[PSQLException](
            hasField("getMessage", _.getMessage(), containsString("OFFSET must not be negative"))
          )
        )
      )
    }
  )

  private val getDIDStateSpec = suite("getManagedDIDState")(
    test("return None of state is not found") {
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        state <- storage.getManagedDIDState(didExample)
      } yield assert(state)(isNone)
    },
    test("return the same state that was set for all variants") {
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        inputs = Seq[(Int, PublicationState)](
          (1, PublicationState.Created()),
          (2, PublicationState.PublicationPending(ArraySeq.fill(32)(0))),
          (3, PublicationState.Published(ArraySeq.fill(32)(1))),
        )
        states <- ZIO.foreach(inputs) { case (didIndex, publicationState) =>
          val operation = generateCreateOperationHdKey(Seq("key-1"), didIndex).map(_._1)
          operation.map(o => ManagedDIDState(o, didIndex, publicationState))
        }
        readStates <- ZIO.foreach(states) { state =>
          for {
            _ <- storage.insertManagedDID(state.createOperation.did, state, Map.empty)
            readState <- storage.getManagedDIDState(state.createOperation.did)
          } yield readState
        }
      } yield assert(readStates.flatten)(hasSameElements(states))
    }
  )

  private val listDIDLineageSpec = {
    val initDIDLineage = {
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        did1 <- initializeDIDStateAndKeys(Nil, 0)
        did2 <- initializeDIDStateAndKeys(Nil, 1)
        input = Seq(
          (did1, Array.fill[Byte](32)(0), ScheduledDIDOperationStatus.Pending),
          (did1, Array.fill[Byte](32)(1), ScheduledDIDOperationStatus.Confirmed),
          (did2, Array.fill[Byte](32)(2), ScheduledDIDOperationStatus.Pending),
          (did2, Array.fill[Byte](32)(3), ScheduledDIDOperationStatus.Confirmed),
        )
        _ <- ZIO.foreach(input) { case (did, hash, status) =>
          storage.insertDIDUpdateLineage(did, updateLineage(operationId = hash, status = status))
        }
      } yield (did1, did2)
    }

    suite("listDIDUpdateLineage")(
      test("initialize with empty lineage") {
        for {
          storage <- ZIO.service[DIDNonSecretStorage]
          lineage <- storage.listUpdateLineage(None, None)
        } yield assert(lineage)(isEmpty)
      },
      test("list all lineage") {
        for {
          _ <- initDIDLineage
          lineage <- ZIO.serviceWithZIO[DIDNonSecretStorage](_.listUpdateLineage(None, None))
        } yield assert(lineage.map(_.operationId))(
          hasSameElements(
            Seq(
              ArraySeq.fill(32)(0),
              ArraySeq.fill(32)(1),
              ArraySeq.fill(32)(2),
              ArraySeq.fill(32)(3),
            )
          )
        )
      },
      test("list lineage filtered by did") {
        for {
          did1 <- initDIDLineage.map(_._1)
          lineage <- ZIO.serviceWithZIO[DIDNonSecretStorage](_.listUpdateLineage(Some(did1), None))
        } yield assert(lineage.map(_.operationId))(
          hasSameElements(Seq(ArraySeq.fill(32)(0), ArraySeq.fill(32)(1)))
        )
      },
      test("list lineage filtered by status") {
        for {
          did1 <- initDIDLineage.map(_._1)
          lineage <- ZIO.serviceWithZIO[DIDNonSecretStorage](
            _.listUpdateLineage(None, Some(ScheduledDIDOperationStatus.Pending))
          )
        } yield assert(lineage.map(_.operationId))(
          hasSameElements(Seq(ArraySeq.fill(32)(0), ArraySeq.fill(32)(2)))
        )
      },
      test("list lineage filtered by did and status") {
        for {
          did1 <- initDIDLineage.map(_._1)
          lineage <- ZIO.serviceWithZIO[DIDNonSecretStorage](
            _.listUpdateLineage(Some(did1), Some(ScheduledDIDOperationStatus.Pending))
          )
        } yield assert(lineage.map(_.operationId))(
          hasSameElements(Seq(ArraySeq.fill(32)(0)))
        )
      }
    )
  }

  private val setDIDLineageStatusSpec = suite("setDIDLineageStatus")(
    test("do not fail on setting non-existing did lineage") {
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        _ <- storage.setDIDUpdateLineageStatus(Array.fill(32)(0), ScheduledDIDOperationStatus.Pending)
      } yield assertCompletes
    },
    test("set status for existing did") {
      val operationId = Array.fill[Byte](32)(42)
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        did <- initializeDIDStateAndKeys(Nil, 0)
        _ <- storage.insertDIDUpdateLineage(
          did,
          updateLineage(operationId = operationId, status = ScheduledDIDOperationStatus.Pending)
        )
        status1 <- storage.listUpdateLineage(Some(did), None).map(_.head.status)
        _ <- storage.setDIDUpdateLineageStatus(operationId, ScheduledDIDOperationStatus.Confirmed)
        status2 <- storage.listUpdateLineage(Some(did), None).map(_.head.status)
      } yield assert(status1)(equalTo(ScheduledDIDOperationStatus.Pending))
        && assert(status2)(equalTo(ScheduledDIDOperationStatus.Confirmed))
    }
  )

}
