package io.iohk.atala.test.container

import zio.*
import io.iohk.atala.shared.test.containers.VaultTestContainer
import io.iohk.atala.shared.test.containers.VaultContainerCustom

object VaultLayer {

  def vaultLayer(vaultToken: String): TaskLayer[VaultContainerCustom] = {
    ZLayer
      .scoped {
        ZIO
          .acquireRelease(ZIO.attemptBlocking {
            VaultTestContainer.vaultContainer(vaultToken = Some(vaultToken))
          })(container => ZIO.attemptBlocking(container.stop()).orDie)
          // Start the container outside the aquireRelease as this might fail
          // to ensure contianer.stop() is added to the finalizer
          .tap(container => ZIO.attemptBlocking(container.start()))
      }
  }

}
