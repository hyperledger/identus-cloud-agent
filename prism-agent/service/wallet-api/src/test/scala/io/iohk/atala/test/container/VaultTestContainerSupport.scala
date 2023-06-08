package io.iohk.atala.test.container

import zio.*
import io.iohk.atala.agent.walletapi.vault.VaultKVClient
import io.iohk.atala.agent.walletapi.vault.VaultKVClientImpl
import io.iohk.atala.shared.test.containers.VaultContainerCustom

trait VaultTestContainerSupport {

  private val TEST_TOKEN = "root"

  protected val vaultContainerLayer: ULayer[VaultContainerCustom] = VaultLayer.vaultLayer(vaultToken = TEST_TOKEN)

  protected def vaultKvClientLayer: TaskLayer[VaultKVClient] =
    vaultContainerLayer >>> ZLayer.fromFunction { (container: VaultContainerCustom) =>
      val address = container.getDockerHttpHostAddress()
      ZLayer.fromZIO(VaultKVClientImpl.fromAddressAndToken(address, TEST_TOKEN))
    }.flatten

}
