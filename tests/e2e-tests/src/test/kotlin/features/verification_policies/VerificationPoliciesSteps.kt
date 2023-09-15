package features.verification_policies

import api_models.VerificationPolicy
import api_models.VerificationPolicyInput
import common.TestConstants
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import interactions.Put
import interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import java.util.*


class VerificationPoliciesSteps {

    @When("{actor} creates a new verification policy")
    fun acmeCreatesANewVerificationPolicy(actor: Actor) {
        actor.attemptsTo(
            Post.to("/verification/policies").with {
                it.body(TestConstants.VERIFICATION_POLICIES.VERIFICATION_POLICY)
            },
        )
    }

    @Then("{actor} sees new verification policy is available")
    fun newVerificationPolicyIsAvailable(actor: Actor) {
        actor.should(ResponseConsequence.seeThatResponse("New policy created") {
            it.statusCode(HttpStatus.SC_CREATED)
            //it.body("", CoreMatchers.`is`(Matchers.emptyString()))
            it.body("id", CoreMatchers.not(Matchers.emptyString()))
            it.body("nonce", CoreMatchers.not(Matchers.emptyString()))
            it.body("kind", Matchers.containsString("VerificationPolicy"))
            it.body(
                "name",
                Matchers.containsString(TestConstants.VERIFICATION_POLICIES.VERIFICATION_POLICY.name)
            )
            it.body(
                "description",
                Matchers.containsString(TestConstants.VERIFICATION_POLICIES.VERIFICATION_POLICY.description)
            )
            TestConstants.VERIFICATION_POLICIES.VERIFICATION_POLICY.constraints!!.forEach { constraint ->
                it.body("constraints.schemaId", CoreMatchers.hasItem(constraint.schemaId))
                it.body("constraints.trustedIssuers", CoreMatchers.hasItems(constraint.trustedIssuers!!))
            }
        })
        val policy = SerenityRest.lastResponse().`as`(VerificationPolicy::class.java)
        actor.remember("policy", policy)
    }

    @When("{actor} updates a new verification policy")
    fun acmeUpdatesAVerificationPolicy(actor: Actor) {
        val policy = actor.recall<VerificationPolicy>("policy")
        val updatePolicyInput = VerificationPolicyInput(
            name = policy.name,
            description = "updated description + ${UUID.randomUUID()}",
            constraints = policy.constraints
        )
        actor.attemptsTo(
            Put.to("/verification/policies/${policy.id}?nonce=${policy.nonce}").with {
                it.body(updatePolicyInput)
            },
        )
        actor.remember("updatedPolicyInput", updatePolicyInput)
    }

    @Then("{actor} sees the updated verification policy is available")
    fun updatedVerificationPolicyIsAvailable(actor: Actor) {
        val updatedPolicy = actor.forget<VerificationPolicy>("policy")
        val updatePolicyInput = actor.forget<VerificationPolicyInput>("updatedPolicyInput")
        actor.should(ResponseConsequence.seeThatResponse("Verification policy is updated") {
            it.statusCode(HttpStatus.SC_OK)
            it.body("id", CoreMatchers.`is`(Matchers.equalTo(updatedPolicy.id)))
            it.body("nonce", CoreMatchers.not(Matchers.emptyString()))
            it.body("kind", Matchers.containsString("VerificationPolicy"))
            it.body(
                "name",
                Matchers.containsString(updatePolicyInput.name)
            )
            it.body(
                "description",
                Matchers.containsString(updatePolicyInput.description)
            )
            updatePolicyInput.constraints!!.forEach { constraint ->
                it.body("constraints.schemaId", CoreMatchers.hasItem(constraint.schemaId))
                it.body("constraints.trustedIssuers", CoreMatchers.hasItems(constraint.trustedIssuers!!))
            }
        })
        val policy = SerenityRest.lastResponse().`as`(VerificationPolicy::class.java)
        actor.remember("policy", policy)
    }
}
