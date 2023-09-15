package common

object Environments {
    val AGENT_AUTH_REQUIRED: Boolean = (System.getenv("AGENT_AUTH_REQUIRED") ?: "true").toBoolean()
    val AGENT_AUTH_HEADER = System.getenv("AGENT_AUTH_HEADER") ?: "apikey"
    val ACME_AUTH_KEY = System.getenv("ACME_AUTH_KEY") ?: "ACME_AUTH_KEY"
    val ACME_AGENT_URL = System.getenv("ACME_AGENT_URL") ?: "http://localhost:8080/prism-agent"
    val ACME_AGENT_WEBHOOK_HOST = System.getenv("ACME_AGENT_WEBHOOK_HOST") ?: "host.docker.internal"
    val ACME_AGENT_WEBHOOK_PORT = (System.getenv("ACME_AGENT_WEBHOOK_PORT") ?: "9955").toInt()
    val ACME_AGENT_WEBHOOK_URL = "http://$ACME_AGENT_WEBHOOK_HOST:$ACME_AGENT_WEBHOOK_PORT"
    val BOB_AGENT_URL = System.getenv("BOB_AGENT_URL") ?: "http://localhost:8090/prism-agent"
    val BOB_AUTH_KEY = System.getenv("BOB_AUTH_KEY") ?: "default"
    val BOB_AGENT_WEBHOOK_HOST = System.getenv("BOB_AGENT_WEBHOOK_HOST") ?: "host.docker.internal"
    val BOB_AGENT_WEBHOOK_PORT = (System.getenv("BOB_AGENT_WEBHOOK_PORT") ?: "9956").toInt()
    val BOB_AGENT_WEBHOOK_URL = "http://$BOB_AGENT_WEBHOOK_HOST:$BOB_AGENT_WEBHOOK_PORT"
    val FABER_AGENT_URL = System.getenv("FABER_AGENT_URL") ?: "http://localhost:8080/prism-agent"
    val FABER_AUTH_KEY = System.getenv("FABER_AUTH_KEY") ?: "FABER_AUTH_KEY"
    val FABER_AGENT_WEBHOOK_HOST = System.getenv("FABER_AGENT_WEBHOOK_HOST") ?: "host.docker.internal"
    val FABER_AGENT_WEBHOOK_PORT = (System.getenv("FABER_AGENT_WEBHOOK_PORT") ?: "9957").toInt()
    val FABER_AGENT_WEBHOOK_URL = "http://$FABER_AGENT_WEBHOOK_HOST:$FABER_AGENT_WEBHOOK_PORT"
    val ADMIN_AGENT_URL = ACME_AGENT_URL
    val ADMIN_AUTH_HEADER = System.getenv("ADMIN_AUTH_HEADER") ?: "x-admin-api-key"
    val ADMIN_AUTH_TOKEN = System.getenv("ADMIN_AUTH_TOKEN") ?: "admin"
}
