package features.did

import api_models.*
import common.Utils.lastResponseList
import common.Utils.lastResponseObject
import common.Utils.toJsonPath
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.rest.SerenityRest.lastResponse
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus.SC_OK
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers.*

class ManageDidSteps {

    @Given("{actor} creates {int} managed DIDs")
    fun createsMultipleManagedDids(actor: Actor, number: Int) {
        repeat(number) {
            createManageDid(actor)
        }
        actor.remember("number", number)
    }

    @When("{actor} create a managed DID")
    fun createManageDid(actor: Actor) {
        val createDidRequest = createManagedDidRequest()

        actor.attemptsTo(
            Post.to("/did-registrar/dids")
                .with {
                    it.body(createDidRequest)
                }
        )
        var createdDids = actor.recall<MutableList<String>>("createdDids")
        if (createdDids == null) {
            createdDids = mutableListOf()
        }
        createdDids.add(lastResponseObject("longFormDid", String::class))
        actor.remember("createdDids", createdDids)
    }

    @When("{actor} tries to create a managed DID with missing {word}")
    fun triesToCreateManagedDidWithMissingField(actor: Actor, missingFieldPath: String) {
        val createDidRequest = createManagedDidRequest()
        val requestBody = toJsonPath(createDidRequest).delete(missingFieldPath).jsonString()
        actor.attemptsTo(
            Post.to("/did-registrar/dids")
                .with {
                    it.body(requestBody)
                }
        )
    }

    @When("{actor} tries to create a managed DID with value {word} in {word}")
    fun trisToCreateManagedDidWithValueInField(actor: Actor, value: String, fieldPath: String) {
        val createDidRequest = createManagedDidRequest()
        val requestBody = toJsonPath(createDidRequest).set(fieldPath, value).jsonString()
        actor.attemptsTo(
            Post.to("/did-registrar/dids")
                .with {
                    it.body(requestBody)
                }
        )
    }

    @When("{actor} lists all the managed DIDs")
    fun iListManagedDids(actor: Actor) {
        actor.attemptsTo(
            Get.resource("/did-registrar/dids")
        )
    }

    @Then("{actor} sees the managed DID was created successfully")
    fun theDidShouldBeRegisteredSuccessfully(actor: Actor) {
        actor.should(ResponseConsequence.seeThatResponse {
            it.statusCode(SC_OK)
            it.body("longFormDid", not(emptyString()))
        })
    }

    @Then("{actor} sees the request has failed with error status {int}")
    fun seesTheRequestHasFailedWithErrorStatus(actor: Actor, errorStatusCode: Int) {
        Assertions.assertThat(lastResponse().statusCode).isEqualTo(errorStatusCode)
    }

    @Then("{actor} sees the list contains all created DIDs")
    fun seeTheListContainsAllCreatedDids(actor: Actor) {
        val expectedDidsCount = actor.recall<Int>("number")
        val expectedDids = actor.recall<List<String>>("createdDids")
        val managedDidList = lastResponseList("", ManagedDid::class)
        Assertions.assertThat(managedDidList)
            .filteredOn {
                expectedDids.contains(it.longFormDid) && it.status == "CREATED"
            }
            .hasSize(expectedDidsCount)
    }

    private fun createManagedDidRequest(): CreateManagedDidRequest {
        val publicKeys = listOf(PublicKey("123", Purpose.AUTHENTICATION))
        val services = listOf(Service("did:prism:321", "MediatorService", listOf("https://foo.bar.com")))
        val documentTemplate = DocumentTemplate(publicKeys, services)
        return CreateManagedDidRequest(documentTemplate)
    }
}
