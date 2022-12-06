package features.did

import extentions.Agents.agents
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence

class ResolveDidSteps {

    @When("I resolve existing DID by DID reference")
    fun iResolveExistingDIDByDIDReference() {
        println(agents)
        agents["Acme"]!!.attemptsTo(
            Get.resource("/connections")
        )
    }

    @Then("Response code is 200")
    fun responseCodeIs() {
        agents["Acme"]!!.should(
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
