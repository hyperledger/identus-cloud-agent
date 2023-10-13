package features.did

import interactions.Get
import interactions.Post
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.extensions.toJsonPath
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.iohk.atala.prism.models.*
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.SC_CREATED

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
                }
        )

        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED)
        )

        var createdDids = actor.recall<MutableList<String>>("createdDids")
        if (createdDids == null) {
            createdDids = mutableListOf()
        }

        val managedDid = SerenityRest.lastResponse().get<ManagedDID>()

        createdDids.add(managedDid.longFormDid!!)
        actor.remember("createdDids", createdDids)
    }

    @When("{actor} tries to create PRISM DID with missing {word}")
    fun triesToCreateManagedDidWithMissingField(actor: Actor, missingFieldPath: String) {
        val createDidRequest = createPrismDidRequest()
        val requestBody = createDidRequest.toJsonPath().delete(missingFieldPath).jsonString()
        actor.attemptsTo(
            Post.to("/did-registrar/dids")
                .with {
                    it.body(requestBody)
                }
        )
    }

    @When("{actor} tries to create a managed DID with value {word} in {word}")
    fun trisToCreateManagedDidWithValueInField(actor: Actor, value: String, fieldPath: String) {
        val createDidRequest = createPrismDidRequest()
        val requestBody = createDidRequest.toJsonPath().set(fieldPath, value).jsonString()
        actor.attemptsTo(
            Post.to("/did-registrar/dids")
                .with {
                    it.body(requestBody)
                }
        )
    }

    @When("{actor} lists all PRISM DIDs")
    fun iListManagedDids(actor: Actor) {
        actor.attemptsTo(
            Get.resource("/did-registrar/dids")
        )
    }

    @Then("{actor} sees PRISM DID was created successfully")
    fun theDidShouldBeRegisteredSuccessfully(actor: Actor) {
        val managedDid = SerenityRest.lastResponse().get<ManagedDID>()
        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
            Ensure.that(managedDid.longFormDid!!).isNotEmpty()
        )
    }

    @Then("{actor} sees the request has failed with error status {int}")
    fun seesTheRequestHasFailedWithErrorStatus(actor: Actor, errorStatusCode: Int) {
        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(errorStatusCode)
        )
    }

    @Then("{actor} sees the list contains all created DIDs")
    fun seeTheListContainsAllCreatedDids(actor: Actor) {
        val expectedDids = actor.recall<List<String>>("createdDids")
        val managedDidList = SerenityRest.lastResponse().get<ManagedDIDPage>().contents!!
            .filter { it.status == "CREATED" }.map { it.longFormDid!! }
        actor.attemptsTo(
            Ensure.that(managedDidList).containsElementsFrom(expectedDids)
        )
    }

    private fun createPrismDidRequest(): CreateManagedDidRequest = CreateManagedDidRequest(
        CreateManagedDidRequestDocumentTemplate(
            publicKeys = listOf(ManagedDIDKeyTemplate("auth-1", Purpose.authentication)),
            services = listOf(
                Service("https://foo.bar.com", listOf("LinkedDomains"), Json("https://foo.bar.com/"))
            )
        )
    )
}
