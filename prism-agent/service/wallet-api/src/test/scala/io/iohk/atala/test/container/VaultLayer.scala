package io.iohk.atala.test.container

import zio.*
import io.iohk.atala.shared.test.containers.VaultTestContainer
import io.iohk.atala.shared.test.containers.VaultContainerCustom

object VaultLayer {

  def vaultLayer(vaultToken: String): ZLayer[Any, Nothing, VaultContainerCustom] = {
    ZLayer.scoped {
      ZIO.acquireRelease(ZIO.attemptBlocking {
        val container = VaultTestContainer.vaultContainer(vaultToken = Some(vaultToken))
        container.start()
        container
      }.orDie)(container => ZIO.attemptBlocking(container.stop()).orDie)
    }
  }

}
