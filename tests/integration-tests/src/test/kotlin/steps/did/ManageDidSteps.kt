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
import org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY
import org.hyperledger.identus.client.models.*

class ManageDidSteps {

    @Given("{actor} creates {int} PRISM DIDs")
    fun createsMultipleManagedDids(actor: Actor, number: Int) {
        repeat(number) {
            createManageDidWithSecp256k1Key(actor)
        }
        actor.remember("number", number)
    }

    @When("{actor} creates PRISM DID")
    fun createManageDidWithSecp256k1Key(actor: Actor) {
        createManageDid(actor, Curve.SECP256K1, Purpose.AUTHENTICATION)
    }

    @When("{actor} creates PRISM DID with {curve} key having {purpose} purpose")
    fun createManageDid(actor: Actor, curve: Curve, purpose: Purpose) {
        val createDidRequest = createPrismDidRequest(curve, purpose)

        actor.attemptsTo(
            Post.to("/did-registrar/dids")
                .with {
                    it.body(createDidRequest)
                },
        )

        if (SerenityRest.lastResponse().statusCode() == SC_CREATED) {
            var createdDids = actor.recall<MutableList<String>>("createdDids")
            if (createdDids == null) {
                createdDids = mutableListOf()
            }

            val managedDid = SerenityRest.lastResponse().get<ManagedDID>()

            createdDids.add(managedDid.longFormDid!!)
            actor.remember("createdDids", createdDids)
        }
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

    @Then("{actor} sees PRISM DID was not successfully created")
    fun theDidShouldNotBeRegisteredSuccessfully(actor: Actor) {
        val error = SerenityRest.lastResponse().get<ErrorResponse>()
        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_UNPROCESSABLE_ENTITY),
            Ensure.that(error.detail ?: "").isNotEmpty(),
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

    private fun createPrismDidRequest(curve: Curve, purpose: Purpose): CreateManagedDidRequest = CreateManagedDidRequest(
        CreateManagedDidRequestDocumentTemplate(
            publicKeys = listOf(ManagedDIDKeyTemplate("auth-1", purpose, curve)),
            services = listOf(
                Service("https://foo.bar.com", listOf("LinkedDomains"), Json("https://foo.bar.com/")),
            ),
        ),
    )
}
