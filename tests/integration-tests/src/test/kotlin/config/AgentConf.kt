package config

import com.sksamuel.hoplite.ConfigAlias
import java.net.URL

data class AgentConf(
    val url: URL,
    val apikey: String?,
    @ConfigAlias("webhook_url") val webhookUrl: URL?,
    val init: AgentInitConf?,
)
