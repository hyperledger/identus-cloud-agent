package features.system

import interactions.Get
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.iohk.atala.prism.models.HealthInfo
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus

class SystemSteps {
    @When("{actor} makes a request to the health endpoint")
    fun actorRequestsHealthEndpoint(actor: Actor) {
        actor.attemptsTo(
            Get.resource("/_system/health")
        )
        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK)
        )
    }

    @Then("{actor} knows what version of the service is running")
    fun actorUnderstandsVersion(actor: Actor) {
        val healthResponse = SerenityRest.lastResponse().get<HealthInfo>()
        actor.attemptsTo(
            Ensure.that(healthResponse.version).isNotBlank()
        )
    }
}
