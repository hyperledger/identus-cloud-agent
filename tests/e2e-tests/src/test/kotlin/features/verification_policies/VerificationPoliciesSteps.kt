package features.verification_policies

import api_models.VerificationPolicy
import api_models.VerificationPolicyInput
import common.TestConstants
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.interactions.Put
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
        Thread.sleep(10000)
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
        Thread.sleep(10000)
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

//    @When("{actor} creates {int} new schemas")
//    fun acmeCreatesMultipleSchemas(actor: Actor, numberOfSchemas: Int) {
//        val createdSchemas: MutableList<CredentialSchema> = mutableListOf()
//        repeat(numberOfSchemas) { i: Int ->
//            actor.attemptsTo(
//                Post.to("/schema-registry/schemas").with {
//                    it.body(TestConstants.CREDENTIAL_SCHEMAS.generate_with_name_suffix(i.toString()))
//                },
//            )
//            actor.should(
//                ResponseConsequence.seeThatResponse("New schema created") {
//                    it.statusCode(HttpStatus.SC_CREATED)
//                },
//            )
//            createdSchemas.add(Utils.lastResponseObject("", CredentialSchema::class))
//        }
//        actor.remember("createdSchemas", createdSchemas)
//    }
//
//    @Then("{actor} can access all of them one by one")
//    fun theyCanBeAccessedWithPagination(actor: Actor) {
//        actor.recall<List<CredentialSchema>>("createdSchemas").forEach { schema ->
//            actor.attemptsTo(
//                Get.resource("/schema-registry/schemas/${schema.guid}"),
//            )
//            actor.should(
//                ResponseConsequence.seeThatResponse("Schema achieved") {
//                    it.statusCode(HttpStatus.SC_OK)
//                },
//            )
//        }
//    }

//    @When("{actor} creates a new schema with some id")
//    fun acmeCreatesANewSchemaWithFixedId(actor: Actor) {
//        val wrongSchema = TestConstants.CREDENTIAL_SCHEMAS.STUDENT_SCHEMA
//        wrongSchema.guid = TestConstants.RANDOM_CONSTAND_UUID
//        actor.attemptsTo(
//            Post.to("/schema-registry/schemas").with {
//                it.body(wrongSchema)
//            },
//        )
//        actor.should(
//            ResponseConsequence.seeThatResponse("New schema created") {
//                it.statusCode(HttpStatus.SC_CREATED)
//            },
//        )
//    }
//
//    @When("{actor} tries to create a new schema with identical id")
//    fun acmeTriesToCreateANewSchemaWithSameId(actor: Actor) {
//        val wrongSchema = TestConstants.CREDENTIAL_SCHEMAS.STUDENT_SCHEMA
//        wrongSchema.guid = TestConstants.RANDOM_CONSTAND_UUID
//        actor.attemptsTo(
//            Post.to("/schema-registry/schemas").with {
//                it.body(wrongSchema)
//            },
//        )
//    }
//
//    @Then("{actor} sees the request failure with identical id error")
//    fun idDuplicateErrorIsThrown(actor: Actor) {
//        try {
//            actor.should(
//                ResponseConsequence.seeThatResponse("New schema creation error: same UUID") {
//                    it.statusCode(HttpStatus.SC_BAD_REQUEST)
//                },
//            )
//        } catch (err: AssertionError) {
//            println(err.message)
//            throw PendingException("BUG: New credential schema CAN be created with same UUID.")
//        }
//    }
//
//    @When("{actor} tries to create a new schema with {word} in field {word}")
//    fun acmeTriesToCreateANewSchemaWithField(actor: Actor, value: String, field: String) {
//        actor.attemptsTo(
//            Post.to("/schema-registry/schemas").with {
//                it.body(
//                    Utils.toJsonPath(TestConstants.CREDENTIAL_SCHEMAS.STUDENT_SCHEMA).set(field, value).jsonString(),
//                )
//            },
//        )
//    }
//
//    @When("{actor} tries to get schemas with {int} in parameter {word}")
//    fun acmeTriesToCreateANewSchemaWithParameter(actor: Actor, value: Int, parameter: String) {
//        actor.attemptsTo(
//            Get.resource("/schema-registry/schemas?$parameter=$value"),
//        )
//    }
//
//    @Then("{actor} sees the request with status {int}")
//    fun heSeesTheRequestFailureWithErrorStatus(actor: Actor, errorStatusCode: Int) {
//        try {
//            actor.should(
//                ResponseConsequence.seeThatResponse {
//                    it.statusCode(errorStatusCode)
//                },
//            )
//        } catch (err: AssertionError) {
//            println(err.message)
//            throw PendingException("BUG: credential schemas CAN be accessed with negative limit and offset.")
//        }
//    }
}