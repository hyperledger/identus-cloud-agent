package common

import io.restassured.path.json.JsonPath
import io.restassured.specification.RequestSpecification
import net.serenitybdd.rest.SerenityRest
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.withPollInterval
import org.awaitility.pollinterval.FixedPollInterval
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration


object Utils {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    /**
    Attach PRISM auth Header to a request specification if environment variable
    is set to true to require it.

    @param   rs: [RequestSpecification] to be modified
    @return  original [RequestSpecification] if auth not needed, modified [RequestSpecification] if needed
     */
    fun attachAuthHeaderIfRequired(rs: RequestSpecification): RequestSpecification {

        return if(Environments.AGENT_AUTH_REQUIRED) {
            logger.info("AGENT_AUTH_REQUIRED set to true")
            rs.header(Environments.AGENT_AUTH_HEADER, Environments.AGENT_AUTH_KEY)
        } else {
            rs
        }
    }

    fun lastResponse(): JsonPath {
        return SerenityRest.lastResponse().jsonPath()
    }

    fun wait(
        blockToWait: () -> Boolean,
        errorMessage: String,
        poolInterval: FixedPollInterval = FixedPollInterval(Duration.ofSeconds(7L)),
        timeout: Duration = Duration.ofSeconds(60L)
    ): Unit {
        try {
            Awaitility.await().withPollInterval(poolInterval)
                .pollInSameThread()
                .atMost(timeout)
                .until {
                    blockToWait()
                }
        } catch (err: ConditionTimeoutException) {
            throw ConditionTimeoutException(
                errorMessage
            )
        }
        return Unit
    }
}
