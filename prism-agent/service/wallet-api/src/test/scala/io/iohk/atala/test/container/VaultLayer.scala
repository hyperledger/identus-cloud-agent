package io.iohk.atala.test.container

import zio.*
import com.dimafeng.testcontainers.VaultContainer
import io.iohk.atala.shared.test.containers.VaultTestContainer

object VaultLayer {

  def vaultLayer(vaultToken: String): ZLayer[Any, Nothing, VaultContainer] = {
    ZLayer.scoped {
      ZIO.acquireRelease(ZIO.attemptBlocking {
        val container = VaultTestContainer.vaultContainer(vaultToken = Some(vaultToken))
        container.start()
        container
      }.orDie)(container => ZIO.attemptBlocking(container.stop()).orDie)
    }
  }

}
