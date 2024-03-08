package config

import com.sksamuel.hoplite.ConfigAlias
import java.net.URL

data class Webhook(
    val url: URL,
    @ConfigAlias("local_port") val localPort: Int? = null,
    @ConfigAlias("init_required") val initRequired: Boolean = true,
)
