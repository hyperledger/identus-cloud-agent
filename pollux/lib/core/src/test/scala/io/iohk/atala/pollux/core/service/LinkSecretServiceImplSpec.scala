package io.iohk.atala.pollux.core.service

import io.iohk.atala.agent.walletapi.memory.GenericSecretStorageInMemory
import io.iohk.atala.agent.walletapi.storage.GenericSecretStorage
import io.iohk.atala.pollux.anoncreds.*
import io.iohk.atala.shared.models.WalletId.*
import io.iohk.atala.shared.models.{WalletAccessContext, WalletId}
import zio.*
import zio.test.*
import zio.test.TestAspect.*

object LinkSecretServiceImplSpec extends ZIOSpecDefault {
  protected val defaultWalletLayer = ZLayer.succeed(WalletAccessContext(WalletId.default))

  protected val linkSecretServiceServiceLayer =
    ZLayer.make[GenericSecretStorage & LinkSecretService & WalletAccessContext](
      GenericSecretStorageInMemory.layer,
      LinkSecretServiceImpl.layer,
      defaultWalletLayer
    )

  override def spec = {
    suite("LinkSecretServiceImpl")(
      test("fetchOrCreate") {
        import LinkSecretServiceImpl.given

        for {
          svc <- ZIO.service[LinkSecretService]
          record <- svc.fetchOrCreate()
          record1 <- svc.fetchOrCreate()
          storage <- ZIO.service[GenericSecretStorage]
          maybeDidSecret <- storage
            .get[String, lib.LinkSecret](LinkSecretServiceImpl.defaultLinkSecretId)
        } yield {
          assertTrue(record.id == LinkSecretServiceImpl.defaultLinkSecretId)
          assertTrue(record == record1)
          assertTrue(maybeDidSecret.map(_.data).contains(record.secret.data))
        }
      }
    ).provide(linkSecretServiceServiceLayer)
  } @@ samples(1)
}
