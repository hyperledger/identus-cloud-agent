package config.services

import com.sksamuel.hoplite.ConfigAlias
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File

data class Agent(
    val version: String,
    @ConfigAlias("http_port") val httpPort: Int,
    @ConfigAlias("didcomm_port") val didcommPort: Int,
    @ConfigAlias("didcomm_service_url") val didcommServiceUrl: String?,
    @ConfigAlias("rest_service_url") val restServiceUrl: String?,
    @ConfigAlias("auth_enabled") val authEnabled: Boolean,
    @ConfigAlias("prism_node") val prismNode: PrismNode?,
    val keycloak: Keycloak?,
    val vault: Vault?,
    @ConfigAlias("keep_running") override val keepRunning: Boolean = false
) : ServiceBase {

    override val env: ComposeContainer = ComposeContainer(
        File("src/test/resources/containers/agent.yml")
    ).withEnv(
        mapOf(
            "OPEN_ENTERPRISE_AGENT_VERSION" to version,
            "API_KEY_ENABLED" to authEnabled.toString(),
            "AGENT_DIDCOMM_PORT" to didcommPort.toString(),
            "DIDCOMM_SERVICE_URL" to (didcommServiceUrl ?: "http://host.docker.internal:${didcommPort}"),
            "AGENT_HTTP_PORT" to httpPort.toString(),
            "REST_SERVICE_URL" to (restServiceUrl ?: "http://host.docker.internal:${httpPort}"),
            "PRISM_NODE_PORT" to (prismNode?.httpPort?.toString() ?: ""),
            "SECRET_STORAGE_BACKEND" to if (vault != null) "vault" else "postgres",
            "VAULT_HTTP_PORT" to (vault?.httpPort?.toString() ?: ""),
            "KEYCLOAK_ENABLED" to (keycloak != null).toString(),
            "KEYCLOAK_HTTP_PORT" to (keycloak?.httpPort?.toString() ?: ""),
            "KEYCLOAK_REALM" to (keycloak?.realm ?: ""),
            "KEYCLOAK_CLIENT_ID" to (keycloak?.clientId ?: ""),
            "KEYCLOAK_CLIENT_SECRET" to (keycloak?.clientSecret ?: "")
        )
    ).waitingFor("open-enterprise-agent", Wait.forHealthcheck())
}
