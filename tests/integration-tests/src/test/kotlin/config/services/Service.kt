package config.services

import com.sksamuel.hoplite.ConfigAlias

data class Service(
    @ConfigAlias("prism_node") val prismNode: PrismNode?,
    val keycloak: Keycloak?,
    val vault: Vault?,
)
