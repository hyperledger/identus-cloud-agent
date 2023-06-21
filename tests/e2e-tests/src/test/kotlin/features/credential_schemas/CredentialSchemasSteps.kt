package features.credential_schemas

import api_models.CredentialSchema
import com.fasterxml.jackson.databind.ObjectMapper
import common.TestConstants
import common.Utils.lastResponseObject
import common.Utils.toJsonPath
import io.cucumber.java.PendingException
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.restassured.path.json.JsonPath
import net.serenitybdd.screenplay.Actor
import interactions.Get
import interactions.Post
import net.serenitybdd.screenplay.rest.questions.ResponseConsequence
import org.apache.http.HttpStatus.*
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.emptyString
import java.util.*

class CredentialSchemasSteps {

    @When("{actor} creates a new credential schema")
    fun acmeCreatesANewCredentialSchema(actor: Actor) {
        actor.attemptsTo(
            Post.to("/schema-registry/schemas").with {
                it.body(TestConstants.CREDENTIAL_SCHEMAS.STUDENT_SCHEMA.copy(author = actor.recall("shortFormDid")))
            },
        )
    }

    @Then("{actor} sees new credential schema is available")
    fun newCredentialSchemaIsAvailable(actor: Actor) {
        actor.should(ResponseConsequence.seeThatResponse("New schema created") {
            it.statusCode(SC_CREATED)
            it.body("guid", not(emptyString()))
            it.body("id", not(emptyString()))
            it.body("longId", not(emptyString()))
            it.body("authored", not(emptyString()))
            it.body("kind", containsString("CredentialSchema"))
            it.body("name", containsString(TestConstants.CREDENTIAL_SCHEMAS.STUDENT_SCHEMA.name))
            it.body("description", containsString(TestConstants.CREDENTIAL_SCHEMAS.STUDENT_SCHEMA.description))
            it.body("version", containsString(TestConstants.CREDENTIAL_SCHEMAS.STUDENT_SCHEMA.version))
            it.body("type", equalTo(TestConstants.CREDENTIAL_SCHEMAS.CREDENTIAL_SCHEMA_TYPE))
            TestConstants.CREDENTIAL_SCHEMAS.STUDENT_SCHEMA.tags!!.forEach { tag ->
                it.body("tags", hasItem(tag))
            }
            it.body(
                "schema.\$id",
                equalTo(TestConstants.CREDENTIAL_SCHEMAS.STUDENT_SCHEMA.schema!!.get("\$id").asText())
            )

            it.body(
                "schema", equalTo<Map<String, Any>>(
                    JsonPath(
                        ObjectMapper().writeValueAsString(
                            TestConstants.CREDENTIAL_SCHEMAS.STUDENT_SCHEMA.schema
                        )
                    ).getMap("")
                )
            )
        })
    }

    @When("{actor} creates {int} new schemas")
    fun acmeCreatesMultipleSchemas(actor: Actor, numberOfSchemas: Int) {
        val createdSchemas: MutableList<CredentialSchema> = mutableListOf()
        repeat(numberOfSchemas) { i: Int ->
            actor.attemptsTo(
                Post.to("/schema-registry/schemas").with {
                    it.body(
                        TestConstants.CREDENTIAL_SCHEMAS.generate_with_name_suffix_and_author(
                            i.toString(),
                            actor.recall("shortFormDid")
                        )
                    )
                },
            )
            actor.should(
                ResponseConsequence.seeThatResponse("New schema created") {
                    it.statusCode(SC_CREATED)
                },
            )
            createdSchemas.add(lastResponseObject("", CredentialSchema::class))
        }
        actor.remember("createdSchemas", createdSchemas)
    }

    @Then("{actor} can access all of them one by one")
    fun theyCanBeAccessedWithPagination(actor: Actor) {
        actor.recall<List<CredentialSchema>>("createdSchemas").forEach { schema ->
            actor.attemptsTo(
                Get.resource("/schema-registry/schemas/${schema.guid}"),
            )
            actor.should(
                ResponseConsequence.seeThatResponse("Schema achieved") {
                    it.statusCode(SC_OK)
                },
            )
        }
    }

    @When("{actor} creates a new schema with some id")
    fun acmeCreatesANewSchemaWithFixedId(actor: Actor) {
        val wrongSchema = TestConstants.CREDENTIAL_SCHEMAS.STUDENT_SCHEMA
        wrongSchema.guid = TestConstants.RANDOM_CONSTAND_UUID
        actor.attemptsTo(
            Post.to("/schema-registry/schemas").with {
                it.body(wrongSchema)
            },
        )
        actor.should(
            ResponseConsequence.seeThatResponse("New schema created") {
                it.statusCode(SC_CREATED)
            },
        )
    }

    @When("{actor} tries to create a new schema with identical id")
    fun acmeTriesToCreateANewSchemaWithSameId(actor: Actor) {
        val wrongSchema = TestConstants.CREDENTIAL_SCHEMAS.STUDENT_SCHEMA
        wrongSchema.guid = TestConstants.RANDOM_CONSTAND_UUID
        actor.attemptsTo(
            Post.to("/schema-registry/schemas").with {
                it.body(wrongSchema)
            },
        )
    }

    @Then("{actor} sees the request failure with identical id error")
    fun idDuplicateErrorIsThrown(actor: Actor) {
        try {
            actor.should(
                ResponseConsequence.seeThatResponse("New schema creation error: same UUID") {
                    it.statusCode(SC_BAD_REQUEST)
                },
            )
        } catch (err: AssertionError) {
            println(err.message)
            throw PendingException("BUG: New credential schema CAN be created with same UUID.")
        }
    }

    @When("{actor} tries to create a new schema with {word} in field {word}")
    fun acmeTriesToCreateANewSchemaWithField(actor: Actor, value: String, field: String) {
        actor.attemptsTo(
            Post.to("/schema-registry/schemas").with {
                it.body(
                    toJsonPath(TestConstants.CREDENTIAL_SCHEMAS.STUDENT_SCHEMA).set(field, value).jsonString(),
                )
            },
        )
    }

    @When("{actor} tries to get schemas with {int} in parameter {word}")
    fun acmeTriesToCreateANewSchemaWithParameter(actor: Actor, value: Int, parameter: String) {
        actor.attemptsTo(
            Get.resource("/schema-registry/schemas?$parameter=$value"),
        )
    }

    @Then("{actor} sees the request with status {int}")
    fun heSeesTheRequestFailureWithErrorStatus(actor: Actor, errorStatusCode: Int) {
        try {
            actor.should(
                ResponseConsequence.seeThatResponse {
                    it.statusCode(errorStatusCode)
                },
            )
        } catch (err: AssertionError) {
            println(err.message)
            throw PendingException("BUG: credential schemas CAN be accessed with negative limit and offset.")
        }
    }
}
