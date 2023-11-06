package io.iohk.atala.sharedtest.containers

import com.dimafeng.testcontainers.SingleContainer
import dasniko.testcontainers.keycloak.ExtendableKeycloakContainer
import io.iohk.atala.sharedtest.containers.KeycloakTestContainer.keycloakContainer
import org.testcontainers.utility.DockerImageName
import zio.{TaskLayer, ZIO, ZLayer}

final class KeycloakContainerCustom(
    dockerImageNameOverride: DockerImageName,
    isOnGithubRunner: Boolean = false
) extends SingleContainer[ExtendableKeycloakContainer[_]] {

  private val keycloakContainer: ExtendableKeycloakContainer[_] = new ExtendableKeycloakContainer(
    dockerImageNameOverride.toString
  ) {
    override def getHost: String = {
      if (isOnGithubRunner) super.getContainerId.take(12)
      else super.getHost
    }

    override def getMappedPort(originalPort: Int): Integer = {
      if (isOnGithubRunner) 8080
      else super.getMappedPort(originalPort)
    }
  }

  override val container: ExtendableKeycloakContainer[_] = keycloakContainer
}

object KeycloakContainerCustom {
  val layer: TaskLayer[KeycloakContainerCustom] =
    ZLayer.scoped {
      ZIO
        .acquireRelease(ZIO.attemptBlockingIO {
          keycloakContainer()
        })(container => ZIO.attemptBlockingIO(container.stop()).orDie)
        .tap(container => ZIO.attemptBlocking(container.start()))
    }
}
