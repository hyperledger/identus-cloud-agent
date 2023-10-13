package config

import com.sksamuel.hoplite.ConfigAlias

data class GlobalConf(
    @ConfigAlias("auth_required") val authRequired: Boolean,
    @ConfigAlias("auth_header") val authHeader: String,
    @ConfigAlias("admin_auth_header") val adminAuthHeader: String
)
