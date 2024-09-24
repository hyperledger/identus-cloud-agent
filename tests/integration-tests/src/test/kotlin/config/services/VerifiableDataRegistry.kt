package config.services

import com.sksamuel.hoplite.ConfigAlias
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File

data class VerifiableDataRegistry(
    @ConfigAlias("http_port") val httpPort: Int,
    val version: String,
) : ServiceBase() {
    override val logServices: List<String> = listOf("prism-node")
    private val vdrComposeFile = "src/test/resources/containers/vdr.yml"
    override val container: ComposeContainer = ComposeContainer(File(vdrComposeFile)).withEnv(
        mapOf(
            "PRISM_NODE_VERSION" to version,
            "PRISM_NODE_PORT" to httpPort.toString(),
        ),
    ).waitingFor(
        "prism-node",
        Wait.forLogMessage(".*Server started, listening on.*", 1),
    )
}
