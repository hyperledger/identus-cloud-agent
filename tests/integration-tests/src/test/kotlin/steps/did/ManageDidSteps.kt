package steps.did

import interactions.Get
import interactions.Post
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.SC_CREATED
import org.hyperledger.identus.client.models.CreateManagedDidRequest
import org.hyperledger.identus.client.models.CreateManagedDidRequestDocumentTemplate
import org.hyperledger.identus.client.models.Json
import org.hyperledger.identus.client.models.ManagedDID
import org.hyperledger.identus.client.models.ManagedDIDKeyTemplate
import org.hyperledger.identus.client.models.ManagedDIDPage
import org.hyperledger.identus.client.models.Purpose
import org.hyperledger.identus.client.models.Service

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

        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )

        var createdDids = actor.recall<MutableList<String>>("createdDids")
        if (createdDids == null) {
            createdDids = mutableListOf()
        }

        val managedDid = SerenityRest.lastResponse().get<ManagedDID>()

        createdDids.add(managedDid.longFormDid!!)
        actor.remember("createdDids", createdDids)
    }

    @When("{actor} lists all PRISM DIDs")
    fun iListManagedDids(actor: Actor) {
        actor.attemptsTo(
            Get.resource("/did-registrar/dids"),
        )
    }

    @Then("{actor} sees PRISM DID was created successfully")
    fun theDidShouldBeRegisteredSuccessfully(actor: Actor) {
        val managedDid = SerenityRest.lastResponse().get<ManagedDID>()
        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
            Ensure.that(managedDid.longFormDid!!).isNotEmpty(),
        )
    }

    @Then("{actor} sees the list contains all created DIDs")
    fun seeTheListContainsAllCreatedDids(actor: Actor) {
        val expectedDids = actor.recall<List<String>>("createdDids")
        val managedDidList = SerenityRest.lastResponse().get<ManagedDIDPage>().contents!!
            .filter { it.status == "CREATED" }.map { it.longFormDid!! }
        actor.attemptsTo(
            Ensure.that(managedDidList).containsElementsFrom(expectedDids),
        )
    }

    private fun createPrismDidRequest(): CreateManagedDidRequest = CreateManagedDidRequest(
        CreateManagedDidRequestDocumentTemplate(
            publicKeys = listOf(ManagedDIDKeyTemplate("auth-1", Purpose.AUTHENTICATION)),
            services = listOf(
                Service("https://foo.bar.com", listOf("LinkedDomains"), Json("https://foo.bar.com/")),
            ),
        ),
    )
}
