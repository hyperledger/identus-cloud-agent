package features.did

import api_models.*
import extensions.getRootObject
import extentions.WithAgents
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus.SC_OK
import org.assertj.core.api.Assertions

class RegisterDidSteps : WithAgents() {
    @When("I register a DID document")
    fun iRegisterADidDocument() {
        val publicKeys = listOf(PublicKey("123", Purpose.AUTHENTICATION))
        val services = listOf(Service("did:prism:321", "MediatorService", "https://foo.bar.com"))
        val documentTemplate = DocumentTemplate(publicKeys, services)
        val createDidRequest = CreateManagedDidRequest(documentTemplate)

        acme.attemptsTo(
            Post.to("/did-registrar/dids")
                .with {
                    it.body(createDidRequest)
                }
        )
    }

    @Then("the DID should be registered successfully")
    fun theDidShouldBeRegisteredSuccessfully() {
        acme.should(ResponseConsequence.seeThatResponse {
            it.statusCode(SC_OK)
        })
        val createManageDidResponse = SerenityRest.lastResponse().getRootObject(CreateManagedDidResponse::class)
        Assertions.assertThat(createManageDidResponse.longFormDid).isNotEmpty

        acme.remember("generatedDid", createManageDidResponse.longFormDid)
    }
}
