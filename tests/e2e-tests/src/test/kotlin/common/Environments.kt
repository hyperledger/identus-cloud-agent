package common

object Environments {
    val AGENT_AUTH_REQUIRED: Boolean = System.getenv("AGENT_AUTH_REQUIRED").toBoolean()
    val AGENT_AUTH_HEADER = System.getenv("AGENT_AUTH_HEADER") ?: "apikey"
    val AGENT_AUTH_KEY = System.getenv("AGENT_AUTH_KEY") ?: ""
    val ACME_AGENT_URL = System.getenv("ACME_AGENT_URL") ?: "http://localhost:8080/prism-agent"
    val BOB_AGENT_URL = System.getenv("BOB_AGENT_URL") ?: "http://localhost:8090/prism-agent"
    val MALLORY_AGENT_URL = System.getenv("MALLORY_AGENT_URL") ?: "http://localhost:8100/prism-agent"
    val FABER_AGENT_URL = System.getenv("FABER_AGENT_URL") ?: "http://localhost:8100/prism-agent"
}
