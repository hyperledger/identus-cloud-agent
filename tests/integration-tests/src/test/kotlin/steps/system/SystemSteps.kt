package steps.system

import interactions.Get
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus
import org.hyperledger.identus.client.models.HealthInfo

class SystemSteps {
    @When("{actor} makes a request to the health endpoint")
    fun actorRequestsHealthEndpoint(actor: Actor) {
        actor.attemptsTo(
            Get.resource("/_system/health"),
        )
        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK),
        )
    }

    @Then("{actor} knows what version of the service is running")
    fun actorUnderstandsVersion(actor: Actor) {
        val healthResponse = SerenityRest.lastResponse().get<HealthInfo>()
        actor.attemptsTo(
            Ensure.that(healthResponse.version).isNotBlank(),
        )
    }

    @When("{actor} makes a request to the metrics endpoint")
    fun actorRequestsMetricEndpoint(actor: Actor) {
        actor.attemptsTo(
            Get.resource("/_system/metrics"),
        )
        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK),
        )
    }

    @Then("{actor} sees that the metrics contain background job stats")
    fun actorSeesMetrics(actor: Actor) {
        val metricsResponse = SerenityRest.lastResponse()
        actor.attemptsTo(
            Ensure.that(metricsResponse.body.asString()).contains("present_proof_flow_did_com_exchange_job_ms_gauge"),
            Ensure.that(metricsResponse.body.asString()).contains("connection_flow_did_com_exchange_job_ms_gauge"),
            Ensure.that(metricsResponse.body.asString()).contains("issuance_flow_did_com_exchange_job_ms_gauge"),
        )
    }
}
