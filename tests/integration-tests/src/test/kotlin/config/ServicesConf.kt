package config

import com.sksamuel.hoplite.ConfigAlias

data class ServicesConf(
    @ConfigAlias("prism_node") val prismNode: PrismNodeConf?,
    val keycloak: KeycloakConf?,
    val vault: VaultConf?,
)

data class PrismNodeConf(
    @ConfigAlias("http_port") val httpPort: Int,
    val version: String,
)

data class KeycloakConf(
    @ConfigAlias("http_port") val httpPort: Int,
    val realm: String,
    @ConfigAlias("client_id") val clientId: String,
    @ConfigAlias("client_secret") val clientSecret: String,
    val users: List<KeycloakUser>
)

data class KeycloakUser(
    val username: String,
    val password: String
)

data class VaultConf(
    @ConfigAlias("http_port") val httpPort: Int,
)
