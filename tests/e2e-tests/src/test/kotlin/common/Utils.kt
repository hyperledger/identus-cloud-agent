package common

import io.restassured.path.json.JsonPath
import net.serenitybdd.rest.SerenityRest
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.withPollInterval
import org.awaitility.pollinterval.FixedPollInterval
import java.time.Duration

object Utils {

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