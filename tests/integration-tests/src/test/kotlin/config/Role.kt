package config

import java.net.URL

data class Role(
    val name: String,
    val url: URL,
    val apikey: String?,
    val token: String?,
    val authHeader: String = "apikey",
    val webhook: Webhook?,
    val agentRole: AgentRole?,
    val oid4vciAuthServer: URL?,
)
