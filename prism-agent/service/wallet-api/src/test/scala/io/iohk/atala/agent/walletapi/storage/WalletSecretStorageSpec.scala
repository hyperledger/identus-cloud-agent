package io.iohk.atala.agent.walletapi.storage

import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.iohk.atala.test.container.DBTestUtils
import io.iohk.atala.test.container.PostgresTestContainerSupport
import io.iohk.atala.shared.models.WalletId
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.agent.walletapi.sql.JdbcWalletSecretStorage
import io.iohk.atala.agent.walletapi.model.WalletSeed

object WalletSecretStorageSpec extends ZIOSpecDefault, PostgresTestContainerSupport {

  override def spec = {
    def testSuite(name: String) = suite(name)(
      setWalletSeedSpec
    ) @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)

    val suite1 = testSuite("jdbc as storage")
      .provide(
        JdbcWalletSecretStorage.layer,
        transactorLayer,
        pgContainerLayer
      )

    suite("WalletSecretStorage")(suite1)
  } @@ TestAspect.tag("dev")

  private def setWalletSeedSpec = suite("setWalletSeed")(
    test("set seed on new wallet") {
      val walletAccessCtx = ZLayer.succeed(WalletAccessContext(WalletId.random))
      for {
        storage <- ZIO.service[WalletSecretStorage]
        seed = WalletSeed.fromByteArray(Array.fill[Byte](64)(0))
        maybeSeed1 <- storage.getWalletSeed.provide(walletAccessCtx)
        _ <- storage.setWalletSeed(seed).provide(walletAccessCtx)
      } yield assert(maybeSeed1)(isNone)
    }
  )

}
