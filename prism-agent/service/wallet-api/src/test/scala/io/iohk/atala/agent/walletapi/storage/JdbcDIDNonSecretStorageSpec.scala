package io.iohk.atala.agent.walletapi.storage

import io.iohk.atala.agent.walletapi.sql.JdbcDIDNonSecretStorage
import io.iohk.atala.test.container.{DBTestUtils, PostgresTestContainerSupport}
import zio.*
import zio.test.*

object JdbcDIDNonSecretStorageSpec extends ZIOSpecDefault, PostgresTestContainerSupport {

  override def spec =
    suite("JdbcDIDNonSecretStorageSpec")(sample).provideSomeLayer(
      pgContainerLayer >+> transactorLayer >+> JdbcDIDNonSecretStorage.layer
    )

  private val sample = test("dummyDB") {
    for {
      storage <- ZIO.service[DIDNonSecretStorage]
      _ <- storage.listManagedDID
    } yield assertCompletes
  } @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)

}
