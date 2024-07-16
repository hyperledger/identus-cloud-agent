package org.hyperledger.identus.sharedtest.containers

import com.dimafeng.testcontainers.SingleContainer
import dasniko.testcontainers.keycloak.ExtendableKeycloakContainer
import org.hyperledger.identus.sharedtest.containers.KeycloakTestContainer.keycloakContainer
import org.testcontainers.utility.DockerImageName
import zio.{RLayer, TaskLayer, ZIO, ZLayer}

import scala.jdk.CollectionConverters.*

final class KeycloakContainerCustom(
    dockerImageNameOverride: DockerImageName,
    isOnGithubRunner: Boolean = false,
    environmentVariables: Map[String, String] = Map.empty
) extends SingleContainer[ExtendableKeycloakContainer[?]] {

  private val keycloakContainer: ExtendableKeycloakContainer[?] = new ExtendableKeycloakContainer(
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

    withEnv(environmentVariables.asJava)
  }

  override val container: ExtendableKeycloakContainer[?] = keycloakContainer
}

enum KeycloakImageType(val imageName: DockerImageName) {
  case Default extends KeycloakImageType(DockerImageName.parse("quay.io/keycloak/keycloak:23.0.7"))
  case Atala
      extends KeycloakImageType(DockerImageName.parse("ghcr.io/input-output-hk/atala-prism-keycloak:1.7.0-snapshot"))
  case OID4VCI
      extends KeycloakImageType(
        DockerImageName
          .parse("ghcr.io/hyperledger/identus-keycloak-plugins:0.2.0")
          .asCompatibleSubstituteFor(Default.imageName)
      )
}

object KeycloakContainerCustom {

  val layer: RLayer[KeycloakImageType, KeycloakContainerCustom] =
    ZLayer.scoped {
      ZIO
        .service[KeycloakImageType]
        .flatMap(keycloakImageType =>
          ZIO
            .acquireRelease(ZIO.attemptBlockingIO {
              keycloakContainer(keycloakImageType.imageName)
            })(container => ZIO.attemptBlockingIO(container.stop()).orDie)
            .tap(container => ZIO.attemptBlocking(container.start()))
        )
    }

  val default: TaskLayer[KeycloakContainerCustom] = ZLayer.succeed(KeycloakImageType.Default) >>> layer
  val atala: TaskLayer[KeycloakContainerCustom] = ZLayer.succeed(KeycloakImageType.Atala) >>> layer
  val oid4vci: TaskLayer[KeycloakContainerCustom] = ZLayer.succeed(KeycloakImageType.OID4VCI) >>> layer
}
