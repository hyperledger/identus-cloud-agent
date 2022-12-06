package extentions

import io.restassured.path.json.JsonPath
import net.serenitybdd.core.annotations.events.BeforeScenario
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.abilities.CallAnApi
import net.thucydides.core.util.EnvironmentVariables
import org.awaitility.Awaitility
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.withPollInterval
import org.awaitility.pollinterval.FixedPollInterval
import java.time.Duration

open class WithAgents {

    protected lateinit var acme: Actor
    protected lateinit var bob: Actor

    @BeforeScenario
    fun acmeAndBobAgents() {
        val theRestApiBaseUrlIssuer = System.getenv("RESTAPI_URL_ISSUER") ?: "http://localhost:8080/prism-agent"
        val theRestApiBaseUrlHolder = System.getenv("RESTAPI_URL_HOLDER") ?: "http://localhost:8090/prism-agent"
        acme = Actor.named("Acme").whoCan(CallAnApi.at(theRestApiBaseUrlIssuer))
        bob = Actor.named("Bob").whoCan(CallAnApi.at(theRestApiBaseUrlHolder))
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
