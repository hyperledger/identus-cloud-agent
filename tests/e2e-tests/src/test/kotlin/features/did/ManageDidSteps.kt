package features.did

import api_models.*
import common.Ensure
import common.TestConstants
import common.Utils.lastResponseList
import common.Utils.lastResponseObject
import common.Utils.toJsonPath
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.rest.SerenityRest.lastResponse
import net.serenitybdd.screenplay.Actor
import interactions.Get
import interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus.SC_CREATED
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers.*

class ManageDidSteps {

    @Given("{actor} creates {int} PRISM DIDs")
    fun createsMultipleManagedDids(actor: Actor, number: Int) {
        repeat(number) {
            createManageDid(actor)
        }
        actor.remember("number", number)
    }

    @When("{actor} creates PRISM DID")
    fun createManageDid(actor: Actor) {
        val createDidRequest = createPrismDidRequest()

        actor.attemptsTo(
            Post.to("/did-registrar/dids")
                .with {
                    it.body(createDidRequest)
                },
        )
        var createdDids = actor.recall<MutableList<String>>("createdDids")
        if (createdDids == null) {
            createdDids = mutableListOf()
        }
        createdDids.add(lastResponseObject("longFormDid", String::class))
        actor.remember("createdDids", createdDids)
    }

    @When("{actor} tries to create PRISM DID with missing {word}")
    fun triesToCreateManagedDidWithMissingField(actor: Actor, missingFieldPath: String) {
        val createDidRequest = createPrismDidRequest()
        val requestBody = toJsonPath(createDidRequest).delete(missingFieldPath).jsonString()
        actor.attemptsTo(
            Post.to("/did-registrar/dids")
                .with {
                    it.body(requestBody)
                },
        )
    }

    @When("{actor} tries to create a managed DID with value {word} in {word}")
    fun trisToCreateManagedDidWithValueInField(actor: Actor, value: String, fieldPath: String) {
        val createDidRequest = createPrismDidRequest()
        val requestBody = toJsonPath(createDidRequest).set(fieldPath, value).jsonString()
        actor.attemptsTo(
            Post.to("/did-registrar/dids")
                .with {
                    it.body(requestBody)
                },
        )
    }

    @When("{actor} lists all PRISM DIDs")
    fun iListManagedDids(actor: Actor) {
        actor.attemptsTo(
            Get.resource("/did-registrar/dids"),
        )
    }

    @Then("{actor} sees PRISM DID was created successfully")
    fun theDidShouldBeRegisteredSuccessfully(actor: Actor) {
        actor.should(
            ResponseConsequence.seeThatResponse {
                it.statusCode(SC_CREATED)
                it.body("longFormDid", not(emptyString()))
            },
        )
    }

    @Then("{actor} sees the request has failed with error status {int}")
    fun seesTheRequestHasFailedWithErrorStatus(actor: Actor, errorStatusCode: Int) {
        Assertions.assertThat(lastResponse().statusCode).isEqualTo(errorStatusCode)
    }

    @Then("{actor} sees the list contains all created DIDs")
    fun seeTheListContainsAllCreatedDids(actor: Actor) {
        val expectedDids = actor.recall<List<String>>("createdDids")
        val managedDidList = lastResponseList("contents.longFormDid", String::class)
        actor.attemptsTo(
            Ensure.that(managedDidList).containsElementsFrom(expectedDids)
        )
    }

    private fun createPrismDidRequest(): CreatePrismDidRequest {
        val publicKeys = listOf(
            TestConstants.PRISM_DID_AUTH_KEY,
        )
        val services = listOf(
            TestConstants.PRISM_DID_SERVICE,
        )
        val documentTemplate = DocumentTemplate(publicKeys, services)
        return CreatePrismDidRequest(documentTemplate)
    }
}
