package io.iohk.atala.agent.walletapi.storage

import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.test.container.{DBTestUtils, PostgresTestContainerSupport}
import io.iohk.atala.agent.walletapi.sql.JdbcDIDSecretStorage
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.castor.core.model.did.PrismDIDOperation

object JdbcDIDSecretStorageSpec extends ZIOSpecDefault, PostgresTestContainerSupport {

  private val didExample = PrismDID.buildLongFormFromOperation(PrismDIDOperation.Create(Nil, Nil, Nil))

  override def spec = {
    val testSuite =
      suite("JdbcDIDSecretStorageSpec")(listKeySpec) @@ TestAspect.before(
        DBTestUtils.runMigrationAgentDB
      )
    testSuite.provideSomeLayer(
      pgContainerLayer >+> transactorLayer >+> JdbcDIDSecretStorage.layer
    )
  }

  private val listKeySpec = suite("listKeys")(
    test("initialize with empty list") {
      for {
        storage <- ZIO.service[DIDSecretStorage]
        keys <- storage.listKeys(didExample)
      } yield assert(keys)(isEmpty)
    }
  )

}
