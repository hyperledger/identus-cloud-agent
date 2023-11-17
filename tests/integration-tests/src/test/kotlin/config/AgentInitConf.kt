package config

import com.sksamuel.hoplite.ConfigAlias

data class AgentInitConf(
    val version: String,
    @ConfigAlias("http_port") val httpPort: Int,
    @ConfigAlias("didcomm_port") val didcommPort: Int,
    @ConfigAlias("secret_storage_backend") val secretStorageBackend: String,
    @ConfigAlias("auth_enabled") val authEnabled: Boolean,
    @ConfigAlias("keycloak_enabled") val keycloakEnabled: Boolean,
)
