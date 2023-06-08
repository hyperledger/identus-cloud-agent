package io.iohk.atala.shared.test.containers

import com.dimafeng.testcontainers.VaultContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.vault

class VaultContainerCustom(
    dockerImageNameOverride: Option[DockerImageName] = None,
    vaultToken: Option[String] = None,
    secrets: Option[VaultContainer.Secrets] = None,
    isOnGithubRunner: Boolean = false
) extends VaultContainer(dockerImageNameOverride, vaultToken, None, secrets) {

  def getDockerHttpHostAddress(): String = {
    // See PostgreSQLContainerCustom.jdbcUrl
    if (isOnGithubRunner) s"http://${containerId.take(12)}:8200"
    else container.getHttpHostAddress()
  }

}
