package common

object Environments {
    val AGENT_AUTH_REQUIRED: Boolean = System.getenv("AGENT_AUTH_REQUIRED").toBoolean()
    val AGENT_AUTH_HEADER = System.getenv("AGENT_AUTH_HEADER") ?: "apikey"
    val ACME_AUTH_KEY = System.getenv("ACME_AUTH_KEY") ?: ""
    val ACME_AGENT_URL = System.getenv("ACME_AGENT_URL") ?: "http://localhost:8080/prism-agent"
    val ACME_AGENT_WEBHOOK_HOST = System.getenv("ACME_AGENT_WEBHOOK_HOST") ?: "0.0.0.0"
    val ACME_AGENT_WEBHOOK_PORT = (System.getenv("ACME_AGENT_WEBHOOK_PORT") ?: "9955").toInt()
    val BOB_AGENT_URL = System.getenv("BOB_AGENT_URL") ?: "http://localhost:8090/prism-agent"
    val BOB_AUTH_KEY = System.getenv("BOB_AUTH_KEY") ?: ""
    val BOB_AGENT_WEBHOOK_HOST = System.getenv("BOB_AGENT_WEBHOOK_HOST") ?: "0.0.0.0"
    val BOB_AGENT_WEBHOOK_PORT = (System.getenv("BOB_AGENT_WEBHOOK_PORT") ?: "9956").toInt()
    val FABER_AGENT_URL = System.getenv("FABER_AGENT_URL") ?: "http://localhost:8100/prism-agent"
    val FABER_AUTH_KEY = System.getenv("FABER_AUTH_KEY") ?: ""
    val FABER_AGENT_WEBHOOK_HOST = System.getenv("FABER_AGENT_WEBHOOK_HOST") ?: "0.0.0.0"
    val FABER_AGENT_WEBHOOK_PORT = (System.getenv("FABER_AGENT_WEBHOOK_PORT") ?: "9957").toInt()
}
