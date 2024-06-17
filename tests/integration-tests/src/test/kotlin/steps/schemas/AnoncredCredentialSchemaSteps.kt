package steps.schemas

import interactions.Post
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import models.AnoncredsSchema
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.abilities.CallAnApi
import org.apache.http.HttpStatus.SC_CREATED
import org.hyperledger.identus.client.models.*
import java.util.UUID

class AnoncredCredentialSchemaSteps {
    @Given("{actor} has an anoncred schema definition")
    fun issuerHasAnAnoncredSchemaDefinition(issuer: Actor) {
        if (issuer.recallAll().containsKey("anoncredsCredentialDefinition")) {
            return
        }
        if (!issuer.recallAll().containsKey("anoncredsSchema")) {
            issuerCreatesAnoncredSchema(issuer)
        }
        issuerCreatesAnoncredCredentialDefinition(issuer)
    }

    @When("{actor} creates anoncred schema")
    fun issuerCreatesAnoncredSchema(issuer: Actor) {
        issuer.attemptsTo(
            Post.to("/schema-registry/schemas")
                .with {
                    it.body(
                        CredentialSchemaInput(
                            author = issuer.recall("shortFormDid"),
                            name = UUID.randomUUID().toString(),
                            description = "Simple student credentials schema",
                            type = "AnoncredSchemaV1",
                            schema = AnoncredsSchema(
                                name = "StudentCredential",
                                version = "1.0",
                                issuerId = issuer.recall("shortFormDid"),
                                attrNames = listOf("name", "age", "sex"),
                            ),
                            tags = listOf("school", "students"),
                            version = "1.0.0",
                        ),
                    )
                },
        )
        issuer.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )
        val schema = SerenityRest.lastResponse().get<CredentialSchemaResponse>()
        issuer.remember("anoncredsSchema", schema)
    }

    @When("{actor} creates anoncred credential definition")
    fun issuerCreatesAnoncredCredentialDefinition(issuer: Actor) {
        val schemaRegistryUrl = issuer.usingAbilityTo(CallAnApi::class.java).resolve("/schema-registry/schemas")
            .replace("localhost", "host.docker.internal")
        issuer.attemptsTo(
            Post.to("/credential-definition-registry/definitions")
                .with {
                    it.body(
                        CredentialDefinitionInput(
                            name = "StudentCredential",
                            version = "1.0.0",
                            schemaId = "$schemaRegistryUrl/${issuer.recall<CredentialSchemaResponse>("anoncredsSchema").guid}/schema",
                            description = "Simple student credentials definition",
                            author = issuer.recall("shortFormDid"),
                            signatureType = "CL",
                            tag = "student",
                            supportRevocation = false,
                        ),
                    )
                },
        )
        issuer.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )
        val credentialDefinition = SerenityRest.lastResponse().get<CredentialDefinitionResponse>()
        issuer.remember("anoncredsCredentialDefinition", credentialDefinition)
    }
}
