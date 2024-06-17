package steps.verificationpolicies

import common.TestConstants
import interactions.Get
import interactions.Post
import interactions.Put
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus
import org.hyperledger.identus.client.models.VerificationPolicyInput
import org.hyperledger.identus.client.models.VerificationPolicyResponse
import java.util.*

class VerificationPoliciesSteps {

    @When("{actor} creates a new verification policy")
    fun acmeCreatesANewVerificationPolicy(actor: Actor) {
        actor.attemptsTo(
            Post.to("/verification/policies").with {
                it.body(
                    TestConstants.TEST_VERIFICATION_POLICY,
                )
            },
        )
        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED),
        )
    }

    @Then("{actor} sees new verification policy is available")
    fun newVerificationPolicyIsAvailable(actor: Actor) {
        val policy = SerenityRest.lastResponse().get<VerificationPolicyResponse>()
        actor.attemptsTo(
            Ensure.that(policy.id).isNotNull(),
            Ensure.that(policy.nonce).isNotNull(),
            Ensure.that(policy.kind).contains("VerificationPolicy"),
            Ensure.that(policy.name).contains(TestConstants.TEST_VERIFICATION_POLICY.name),
            Ensure.that(policy.description).contains(TestConstants.TEST_VERIFICATION_POLICY.description),
        )

        policy.constraints!!.forEach {
            actor.attemptsTo(
                Ensure.that(it.schemaId).isEqualTo(TestConstants.TEST_VERIFICATION_POLICY.constraints!!.first().schemaId),
                Ensure.that(it.trustedIssuers!!)
                    .containsExactlyInAnyOrderElementsFrom(
                        TestConstants.TEST_VERIFICATION_POLICY.constraints!!.first().trustedIssuers!!,
                    ),
            )
        }
        actor.remember("policy", policy)
    }

    @When("{actor} updates a new verification policy")
    fun acmeUpdatesAVerificationPolicy(actor: Actor) {
        val policy = actor.recall<VerificationPolicyResponse>("policy")
        val updatePolicyInput = VerificationPolicyInput(
            name = policy.name,
            description = "updated description + ${UUID.randomUUID()}",
            constraints = policy.constraints,
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
        val updatePolicyInput = actor.forget<VerificationPolicyInput>("updatedPolicyInput")

        actor.attemptsTo(
            Get.resource("/verification/policies/${actor.recall<VerificationPolicyResponse>("policy").id}"),
        )
        val policy = SerenityRest.lastResponse().get<VerificationPolicyResponse>()

        actor.attemptsTo(
            Ensure.that(policy.id).isNotNull(),
            Ensure.that(policy.nonce).isNotNull(),
            Ensure.that(policy.kind).contains("VerificationPolicy"),
            Ensure.that(policy.name).contains(updatePolicyInput.name),
            Ensure.that(policy.description).contains(updatePolicyInput.description),
        )

        policy.constraints!!.forEach {
            actor.attemptsTo(
                Ensure.that(it.schemaId).isEqualTo(updatePolicyInput.constraints!!.first().schemaId),
                Ensure.that(it.trustedIssuers!!)
                    .containsExactlyInAnyOrderElementsFrom(
                        updatePolicyInput.constraints!!.first().trustedIssuers!!,
                    ),
            )
        }
    }
}
