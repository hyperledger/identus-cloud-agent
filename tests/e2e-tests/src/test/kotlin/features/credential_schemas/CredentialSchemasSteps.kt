package features.credential_schemas

import common.Agents.Acme
import common.TestConstants
import common.Utils.lastResponse
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import net.serenitybdd.screenplay.rest.interactions.Get
import net.serenitybdd.screenplay.rest.interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus.*
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.emptyString
import java.util.*

class CredentialSchemasSteps {

    @When("Acme creates a new credential schema")
    fun acmeCreatesANewCredentialSchema() {
        Acme.attemptsTo(
            Post.to("/schema-registry/schemas")
                .with {
                    it.header("Content-Type", "application/json")
                    it.body(TestConstants.CREDENTIAL_SCHEMA)
                }
        )
    }

    @Then("New credential schema is available")
    fun newCredentialSchemaIsAvailable() {
        Acme.should(
            ResponseConsequence.seeThatResponse("New schema created") {
                it.statusCode(SC_CREATED)
                it.body("id", not(emptyString()))
                it.body("authored", not(emptyString()))
                it.body("kind", containsString("VerifiableCredentialSchema"))
                it.body("name", containsString(TestConstants.CREDENTIAL_SCHEMA.name))
                it.body("description", containsString(TestConstants.CREDENTIAL_SCHEMA.description))
                it.body("version", containsString(TestConstants.CREDENTIAL_SCHEMA.version))
                TestConstants.CREDENTIAL_SCHEMA.tags!!.forEach { tag ->
                    it.body("tags", hasItem(tag))
                }
                TestConstants.CREDENTIAL_SCHEMA.attributes!!.forEach { attr ->
                    it.body("attributes", hasItem(attr))
                }
            }
        )
    }

    @When("Acme creates {int} schemas")
    fun acmeCreatesMultipleSchemas(numberOfSchemes: Int) {
        for (i in 0 until numberOfSchemes) {
            Acme.attemptsTo(
                Post.to("/schema-registry/schemas")
                    .with {
                        it.header("Content-Type", "application/json")
                        it.body(TestConstants.CREDENTIAL_SCHEMA)
                    }
            )
            Acme.should(
                ResponseConsequence.seeThatResponse("New schema created") {
                    it.statusCode(SC_CREATED)
                }
            )
        }
    }

    @Then("All {int} schemas can be accessed with pagination {int}")
    fun theyCanBeAccessedWithPagination(numberOfSchemes: Int, pagination: Int) {
        for (i in 0 until numberOfSchemes / 2) {
            val resource = lastResponse().get("next") ?: "/schema-registry/schemas?limit=$pagination"
            Acme.attemptsTo(
                Get.resource(resource)
            )
            Acme.should(
                ResponseConsequence.seeThatResponse("Schemas achieved") {
                    it.statusCode(SC_OK)
                }
            )
        }
    }

    @When("Acme tries to get schemas with negative limit")
    fun acmeTriesToGetSchemasWithNegativeLimit() {
        Acme.attemptsTo(
            Get.resource("/schema-registry/schemas?offset=999")
        )
        Acme.should(
            ResponseConsequence.seeThatResponse("Schemas get failure") {
                it.statusCode(SC_OK)
            }
        )
    }

    @When("Acme creates a new schema with empty id")
    fun acmeCreatesANewSchemaWithEmptyId() {
        val wrongSchema = TestConstants.CREDENTIAL_SCHEMA
        wrongSchema.id = ""
        Acme.attemptsTo(
            Post.to("/schema-registry/schemas")
                .with {
                    it.header("Content-Type", "application/json")
                    it.body(wrongSchema)
                }
        )
    }

    @Then("New schema creation is failed with empty id error")
    fun newSchemaCreationIsFailedWithEmptyIdError() {
        Acme.should(
            ResponseConsequence.seeThatResponse("Get schema invalid id failure") {
                it.statusCode(SC_BAD_REQUEST)
                it.body("msg", containsString("Invalid UUID:  at 'id'"))
            }
        )
    }

    @When("Acme creates a new schema with zero attributes")
    fun acmeCreatesANewSchemaWithZeroAttributes() {
        val wrongSchema = TestConstants.CREDENTIAL_SCHEMA
        wrongSchema.attributes = null
        Acme.attemptsTo(
            Post.to("/schema-registry/schemas")
                .with {
                    it.header("Content-Type", "application/json")
                    it.body(wrongSchema)
                }
        )
    }

    @Then("New schema creation is failed with zero attributes error")
    fun newSchemaCreationIsFailedWithZeroAttributesError() {
        Acme.should(
            ResponseConsequence.seeThatResponse("Get schema invalid attributes failure") {
                it.statusCode(SC_BAD_REQUEST)
            }
        )
    }

    @When("Acme creates a new schema with fixed id")
    fun acmeCreatesANewSchemaWithFixedId() {
        val wrongSchema = TestConstants.CREDENTIAL_SCHEMA
        wrongSchema.id = TestConstants.RANDOM_UUID
        Acme.attemptsTo(
            Post.to("/schema-registry/schemas")
                .with {
                    it.header("Content-Type", "application/json")
                    it.body(wrongSchema)
                }
        )
        Acme.should(
            ResponseConsequence.seeThatResponse("New schema created") {
                it.statusCode(SC_CREATED)
            }
        )
    }

    @When("Acme tries to create a new schema with same id")
    fun acmeTriesToCreateANewSchemaWithSameId() {
        val wrongSchema = TestConstants.CREDENTIAL_SCHEMA
        wrongSchema.id = TestConstants.RANDOM_UUID
        Acme.attemptsTo(
            Post.to("/schema-registry/schemas")
                .with {
                    it.header("Content-Type", "application/json")
                    it.body(wrongSchema)
                }
        )
    }

    @Then("Id duplicate error is thrown")
    fun idDuplicateErrorIsThrown() {
        Acme.should(
            ResponseConsequence.seeThatResponse("New schema creation error: same UUID") {
                it.statusCode(SC_BAD_REQUEST)
            }
        )
    }
}
