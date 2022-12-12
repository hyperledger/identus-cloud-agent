package features.did

import common.Agents.Acme
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import common.Utils.attachAuthHeaderIfRequired

class ResolveDidSteps {

    @When("I resolve existing DID by DID reference")
    fun iResolveExistingDIDByDIDReference() {
        Acme.attemptsTo(
            Get.resource("/connections").with { attachAuthHeaderIfRequired(it) }
        )
    }

    @Then("Response code is 200")
    fun responseCodeIs() {
        Acme.should(
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
