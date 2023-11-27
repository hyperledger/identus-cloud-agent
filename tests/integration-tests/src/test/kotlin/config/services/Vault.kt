package config.services

import com.sksamuel.hoplite.ConfigAlias
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File

data class Vault(
    @ConfigAlias("http_port") val httpPort: Int,
    @ConfigAlias("keep_running") override val keepRunning: Boolean = false
) : ServiceBase {

    private val vaultComposeFile: String = "src/test/resources/containers/vault.yml"
    override val env: ComposeContainer = ComposeContainer(File(vaultComposeFile)).withEnv(
        mapOf(
            "VAULT_PORT" to httpPort.toString()
        )
    ).waitingFor(
        "vault",
        Wait.forHealthcheck()
    )
}
