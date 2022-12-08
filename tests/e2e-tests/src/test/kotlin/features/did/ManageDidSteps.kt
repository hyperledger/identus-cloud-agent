package features.did

import api_models.*
import common.Agents.Acme
import common.Utils
import common.Utils.lastResponse
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus.SC_OK
import org.assertj.core.api.Assertions
import org.hamcrest.Matchers.emptyString
import org.hamcrest.Matchers.not

class ManageDidSteps {
    @When("I create a managed DID")
    fun iRegisterADidDocument() {
        val publicKeys = listOf(PublicKey("123", Purpose.AUTHENTICATION))
        val services = listOf(Service("did:prism:321", "MediatorService", "https://foo.bar.com"))
        val documentTemplate = DocumentTemplate(publicKeys, services)
        val createDidRequest = CreateManagedDidRequest(documentTemplate)

        Acme.attemptsTo(
            Post.to("/did-registrar/dids")
                .with {
                    it.body(createDidRequest)
                }
        )
    }

    @When("I list managed DIDs")
    fun iListManagedDids() {
        Acme.attemptsTo(
            Get.resource("/did-registrar/dids")
        )
    }

    @When("I publish the recently created DID")
    fun iPublishTheRecentlyCreatedDid() {
        Acme.attemptsTo(
            Post.to("/did-registrar/dids/$ref/publications")
        )
    }

    @Then("it should be created successfully")
    fun theDidShouldBeRegisteredSuccessfully() {
        Acme.should(ResponseConsequence.seeThatResponse {
            it.statusCode(SC_OK)
            it.body("longFormDid", not(emptyString()))
        })
        Acme.remember("generatedDid", lastResponse().getString("longFormDid"))
    }

    @Then("it should contain the recently created DID")
    fun itShouldContainTheRecentlyCreatedDid() {
        val managedDidList = Utils.lastResponse().getList<ManagedDid>("")
        Assertions.assertThat(managedDidList)
            .filteredOn {
                it.longFormDid == Acme.recall("generatedDid")
                        && it.status == "CREATED"
            }
            .hasSize(1)
    }
}
