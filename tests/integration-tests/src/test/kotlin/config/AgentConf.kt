package config

import com.sksamuel.hoplite.ConfigAlias
import java.net.URL

data class AgentConf(
    val url: URL,
    @ConfigAlias("webhook_url") val webhookUrl: URL?,
    var apikey: String?
)
