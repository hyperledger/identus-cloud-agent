package io.iohk.atala.shared.test.containers

import com.dimafeng.testcontainers.VaultContainer
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.utility.DockerImageName

object VaultTestContainer {
  def vaultContainer(
      imageName: Option[String] = Some("vault:1.13.2"),
      vaultToken: Option[String] = None,
      verbose: Boolean = false
  ): VaultContainerCustom = {
    val isOnGithubRunner = sys.env.contains("GITHUB_NETWORK")
    val container =
      new VaultContainerCustom(
        dockerImageNameOverride = imageName.map(DockerImageName.parse),
        vaultToken = vaultToken,
        isOnGithubRunner = isOnGithubRunner
      )
    if (verbose) {
      container.container
        .withLogConsumer((t: OutputFrame) => println(t.getUtf8String))
    }
    container
  }
}
