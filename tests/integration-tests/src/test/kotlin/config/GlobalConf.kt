package config

import com.sksamuel.hoplite.ConfigAlias

data class GlobalConf(
    @ConfigAlias("auth_header") val authHeader: String = "apikey",
    @ConfigAlias("admin_auth_header") val adminAuthHeader: String = "x-admin-api-key",
)
