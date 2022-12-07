package features.did

import api_models.ManagedDid
import com.fasterxml.jackson.core.type.TypeReference
import extensions.getRootList
import extensions.getRootObject
import extentions.WithAgents
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.rest.interactions.Get
import org.assertj.core.api.Assertions

class ListDidSteps: WithAgents() {
    @When("I list registered DIDs")
    fun iListRegisteredDids() {
        acme.attemptsTo(
            Get.resource("/did-registrar/dids")
        )
    }

    @Then("the list should contain recently created DID")
    fun theListShouldContainRecentlyCreatedDid() {
        val managedDidList = SerenityRest.lastResponse().getRootList(ManagedDid::class)
        println(">>>>>>>> " + acme.recall("generatedDid"))
        Assertions.assertThat(managedDidList)
            .filteredOn {
                println(it.longFormDid)
                it.longFormDid == acme.recall("generatedDid")
            }
            .isNotEmpty
    }
}
