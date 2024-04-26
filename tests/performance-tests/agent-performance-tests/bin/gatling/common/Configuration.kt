package common

import java.util.logging.Level

/**
 * Configuration for simulations running
 */
object Configuration {
    // Issuer agent
    val ISSUER_AGENT_URL = System.getenv("ISSUER_AGENT_URL") ?: "http://localhost:8080/prism-agent"
    val ISSUER_AGENT_API_KEY: String = System.getenv("ISSUER_AGENT_API_KEY") ?: ""

    // Holder agent
    val HOLDER_AGENT_URL = System.getenv("HOLDER_AGENT_URL") ?: "http://localhost:8090/prism-agent"
    val HOLDER_AGENT_API_KEY: String = System.getenv("HOLDER_AGENT_API_KEY") ?: ""

    // Verbose debugging mode
    val LOGGER_LEVEL: Level = if (System.getenv("VERBOSE_SIMULATION").toBoolean()) Level.INFO else Level.WARNING

    // Interval between executing requests while waiting for some condition to happen, seconds
    const val WAITING_LOOP_PAUSE_INTERVAL: Long = 2L

    // Max iterations of `WAITING_LOOP_PAUSE_INTERVAL` to wait until exit the session
    const val WAITING_LOOP_MAX_ITERATIONS: Int = 15

    // Name of the counter in the waiting loop
    const val WAITING_LOOP_COUNTER_NAME: String = "counter"

    // Random credential to be issued during benchmarks
    // it depends on holderDid session variable (!)
    val RANDOM_CREDENTIAL: String =
        """{
             "schemaId": "schema:1234",
             "subjectId": "#{holderDid}",
             "validityPeriod": 3600,
             "claims": {
               "prop1": "value1",
               "prop2": "value2",
               "prop3": "value3"
             },
             "automaticIssuance": false,
             "awaitConfirmation": false
           }
        """.trimIndent()
}
