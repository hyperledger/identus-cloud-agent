package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.sql.JdbcDIDNonSecretStorage
import io.iohk.atala.test.container.{DBTestUtils, PostgresTestContainerSupport}
import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import scala.collection.compat.immutable.ArraySeq

object JdbcDIDNonSecretStorageSpec extends ZIOSpecDefault, StorageSpecHelper, PostgresTestContainerSupport {

  override def spec = {
    val testSuite =
      suite("JdbcDIDNonSecretStorageSpec")(
        listDIDStateSpec,
        setDIDStateSpec,
        getDIDStateSpec
      ) @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)

    testSuite.provideSomeLayer(
      pgContainerLayer >+> transactorLayer >+> JdbcDIDNonSecretStorage.layer
    )
  } @@ TestAspect.tag("dev")

  private val listDIDStateSpec = suite("listManagedDIDState")(
    test("initialize with empty list") {
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        states <- storage.listManagedDID
      } yield assert(states)(isEmpty)
    },
    test("list stored dids") {
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        did1 = PrismDID.buildCanonicalFromSuffix("0" * 64).toOption.get
        did2 = PrismDID.buildCanonicalFromSuffix("1" * 64).toOption.get
        createOperation1 <- generateCreateOperation(Seq("key-1")).map(_._1)
        createOperation2 <- generateCreateOperation(Seq("key-1")).map(_._1)
        _ <- storage.setManagedDIDState(did1, ManagedDIDState.Created(createOperation1))
        _ <- storage.setManagedDIDState(did2, ManagedDIDState.Created(createOperation2))
        states <- storage.listManagedDID
      } yield assert(states.keys)(hasSameElements(Seq(did1, did2)))
    }
  )

  private val setDIDStateSpec = suite("setManagedDIDState")(
    test("replace state if set for the same did") {
      for {
        storage <- ZIO.service[DIDNonSecretStorage]
        createOperation1 <- generateCreateOperation(Seq("key-1")).map(_._1)
        createOperation2 <- generateCreateOperation(Seq("key-1")).map(_._1)
        _ <- storage.setManagedDIDState(didExample, ManagedDIDState.Created(createOperation1))
        state1 <- storage.getManagedDIDState(didExample)
        _ <- storage.setManagedDIDState(didExample, ManagedDIDState.Created(createOperation2))
        state2 <- storage.getManagedDIDState(didExample)
      } yield assert(state1.get.createOperation)(equalTo(createOperation1))
        && assert(state2.get.createOperation)(equalTo(createOperation2))
        && assert(createOperation1)(not(equalTo(createOperation2)))
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
        createOperation <- generateCreateOperation(Seq("key-1")).map(_._1)
        states = Seq(
          ManagedDIDState.Created(createOperation),
          ManagedDIDState.PublicationPending(createOperation, ArraySeq.fill(32)(0)),
          ManagedDIDState.Published(createOperation, ArraySeq.fill(32)(1)),
        )
        readStates <- ZIO.foreach(states) { state =>
          for {
            _ <- storage.setManagedDIDState(didExample, state)
            readState <- storage.getManagedDIDState(didExample)
          } yield readState
        }
      } yield assert(readStates.flatten)(equalTo(states))
    }
  )

}
