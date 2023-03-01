package features.system

import api_models.HealthInfo
import common.Utils.lastResponseObject
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus.SC_OK
import org.assertj.core.api.Assertions.assertThat

class SystemSteps {
    @When("{actor} makes a request to the health endpoint")
    fun actorRequestsHealthEndpoint(actor: Actor) {
        actor.attemptsTo(
            Get.resource("/_system/health"),
        )
        actor.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(SC_OK)
            },
        )
        val healthResponse = lastResponseObject("", HealthInfo::class)
        assertThat(healthResponse)
            .hasFieldOrProperty("version")
            .hasNoNullFieldsOrProperties()
        actor.remember("version", healthResponse.version)
    }

    @Then("{actor} knows what version of the service is running")
    fun actorUnderstandsVersion(actor: Actor) {
        assertThat(actor.recall<String>("version"))
            .isNotBlank()
    }
}
