package io.iohk.atala.test.container

import zio.*
import com.dimafeng.testcontainers.VaultContainer
import io.iohk.atala.agent.walletapi.vault.VaultKVClient
import io.iohk.atala.agent.walletapi.vault.VaultKVClientImpl

trait VaultTestContainerSupport {

  private val TEST_TOKEN = "root"

  protected val vaultContainerLayer: ULayer[VaultContainer] = VaultLayer.vaultLayer(vaultToken = TEST_TOKEN)

  protected def vaultKvClientLayer: TaskLayer[VaultKVClient] =
    vaultContainerLayer >>> ZLayer.fromFunction { (container: VaultContainer) =>
      val address = container.container.getHttpHostAddress()
      ZLayer.fromZIO(VaultKVClientImpl.fromAddressAndToken(address, TEST_TOKEN))
    }.flatten

}
