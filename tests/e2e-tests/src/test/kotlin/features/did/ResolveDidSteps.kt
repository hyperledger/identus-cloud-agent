package features.did

import io.cucumber.java.Before
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.abilities.CallAnApi
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import net.thucydides.core.util.EnvironmentVariables

class ResolveDidSteps {

    private lateinit var environmentVariables: EnvironmentVariables
    private lateinit var issuer: Actor

    @Before
    fun configureBaseUrl() {
        val theRestApiBaseUrl = environmentVariables.optionalProperty("restapi.baseurl")
            .orElse("http://localhost:8080")
        issuer = Actor.named("Issuer").whoCan(CallAnApi.at(theRestApiBaseUrl))
    }

    @When("I resolve existing DID by DID reference")
    fun iResolveExistingDIDByDIDReference() {
        issuer.attemptsTo(
            Get.resource("/dids/did:prism:123")
        )
    }

    @Then("Response code is 200")
    fun responseCodeIs() {
        issuer.should(
            ResponseConsequence.seeThatResponse("DID has required fields") {
                it.statusCode(200)
            }
        )
    }

    @Then("I achieve standard compatible DID document")
    fun iAchieveStandardCompatibleDIDDocument() {
        // to be filled later when did endpoint works
    }

}
