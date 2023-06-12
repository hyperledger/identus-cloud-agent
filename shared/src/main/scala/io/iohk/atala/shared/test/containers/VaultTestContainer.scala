package io.iohk.atala.shared.test.containers

import com.dimafeng.testcontainers.VaultContainer
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.utility.DockerImageName

object VaultTestContainer {
  def vaultContainer(
      imageName: Option[String] = Some("vault:1.13.2"),
      vaultToken: Option[String] = None,
      verbose: Boolean = false
  ): VaultContainer = {
    val container = new VaultContainer(
      dockerImageNameOverride = imageName.map(DockerImageName.parse),
      vaultToken = vaultToken
    )
    if (verbose) {
      container.container
        .withLogConsumer((t: OutputFrame) => println(t.getUtf8String))
    }
    container
  }
}
