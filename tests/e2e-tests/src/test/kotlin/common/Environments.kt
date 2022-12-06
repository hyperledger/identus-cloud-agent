package common

object Environments {

    val FIRST_AGENT_URL = System.getenv("FIRST_AGENT_URL") ?: "http://localhost:8080/prism-agent"
    val SECOND_AGENT_URL = System.getenv("SECOND_AGENT_URL") ?: "http://localhost:8090/prism-agent"
    val THIRD_AGENT_URL = System.getenv("THIRD_AGENT_URL") ?: "http://localhost:8100/prism-agent"

    val urls: Map<String,String> = mutableMapOf(
        "issuer" to FIRST_AGENT_URL,
        "holder" to SECOND_AGENT_URL,
        "verifier" to THIRD_AGENT_URL
    )
}