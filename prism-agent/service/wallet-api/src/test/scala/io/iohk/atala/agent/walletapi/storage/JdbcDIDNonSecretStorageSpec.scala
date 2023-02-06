package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.sql.JdbcDIDNonSecretStorage
import io.iohk.atala.test.container.{DBTestUtils, PostgresTestContainerSupport}
import zio.*
import zio.test.*

object JdbcDIDNonSecretStorageSpec extends ZIOSpecDefault, PostgresTestContainerSupport {

  override def spec =
    suite("JdbcDIDSecretStorageSpec")(sample).provideSomeLayer(
      pgContainerLayer >+> transactorLayer >+> JdbcDIDNonSecretStorage.layer
    )

  private val sample = test("dummyDB") {
    for {
      storage <- ZIO.service[DIDNonSecretStorage]
      _ <- storage.listManagedDID.debug("listState")
    } yield assertCompletes
  } @@ TestAspect.tag("dev") @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)

}
