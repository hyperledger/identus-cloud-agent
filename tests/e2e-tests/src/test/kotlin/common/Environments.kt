package common

object Environments {
    val ACME_AGENT_URL = System.getenv("ACME_AGENT_URL") ?: "http://localhost:8080/prism-agent"
    val BOB_AGENT_URL = System.getenv("BOB_AGENT_URL") ?: "http://localhost:8090/prism-agent"
    val MALLORY_AGENT_URL = System.getenv("MALLORY_AGENT_URL") ?: "http://localhost:8100/prism-agent"
}