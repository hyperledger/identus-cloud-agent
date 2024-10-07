package steps.oid4vci

import com.google.gson.JsonObject
import common.CredentialSchema
import interactions.Delete
import interactions.Get
import interactions.Post
import interactions.body
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus
import org.apache.http.HttpStatus.SC_OK
import org.hyperledger.identus.client.models.CreateCredentialConfigurationRequest
import org.hyperledger.identus.client.models.CredentialFormat
import org.hyperledger.identus.client.models.CredentialIssuer
import org.hyperledger.identus.client.models.IssuerMetadata
import java.util.UUID

class ManageCredentialConfigSteps {
    @Given("{actor} has {string} credential configuration created from {}")
    fun issuerHasExistingCredentialConfig(issuer: Actor, configurationId: String, schema: CredentialSchema) {
        ManageIssuerSteps().issuerHasExistingCredentialIssuer(issuer)
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers/${credentialIssuer.id}/credential-configurations/$configurationId"),
        )
        if (SerenityRest.lastResponse().statusCode != SC_OK) {
            issuerCreateCredentialConfiguration(issuer, schema, configurationId)
        }
    }

    @When("{actor} uses {} to create a credential configuration {string}")
    fun issuerCreateCredentialConfiguration(issuer: Actor, schema: CredentialSchema, configurationId: String) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        val schemaGuid = issuer.recall<String>(schema.name)
        val baseUrl = issuer.recall<String>("baseUrl")

        issuer.attemptsTo(
            Post.to("/oid4vci/issuers/${credentialIssuer.id}/credential-configurations").body(
                CreateCredentialConfigurationRequest(
                    configurationId = configurationId,
                    format = CredentialFormat.JWT_VC_JSON,
                    schemaId = "$baseUrl/schema-registry/schemas/$schemaGuid/schema",
                ),
            ),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED),
        )
    }

    @When("{actor} deletes {string} credential configuration")
    fun issuerDeletesCredentialConfiguration(issuer: Actor, configurationId: String) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Delete("/oid4vci/issuers/${credentialIssuer.id}/credential-configurations/$configurationId"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
    }

    @When("{actor} deletes a non existent {} credential configuration")
    fun issuerDeletesANonExistentCredentialConfiguration(issuer: Actor, configurationId: String) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Delete("/oid4vci/issuers/${credentialIssuer.id}/credential-configurations/$configurationId"),
        )
    }

    @When("{actor} creates a new credential configuration request")
    fun issuerCreatesANewConfigurationRequest(issuer: Actor) {
        val credentialConfiguration = JsonObject()
        issuer.remember("credentialConfiguration", credentialConfiguration)
    }

    @When("{actor} uses {} issuer id for credential configuration")
    fun issuerUsesIssuerId(issuer: Actor, issuerId: String) {
        if (issuerId == "existing") {
            val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
            issuer.remember("credentialConfigurationId", credentialIssuer.id)
        } else if (issuerId == "wrong") {
            issuer.remember("credentialConfigurationId", UUID.randomUUID().toString())
        }
    }

    @When("{actor} adds '{}' configuration id for credential configuration request")
    fun issuerAddsConfigurationIdToCredentialConfigurationRequest(issuer: Actor, configurationId: String) {
        val credentialIssuer = issuer.recall<JsonObject>("credentialConfiguration")
        val configurationIdProperty = if (configurationId == "null") {
            null
        } else {
            configurationId
        }
        credentialIssuer.addProperty("configurationId", configurationIdProperty)
    }

    @When("{actor} adds '{}' format for credential configuration request")
    fun issuerAddsFormatToCredentialConfigurationRequest(issuer: Actor, format: String) {
        val credentialIssuer = issuer.recall<JsonObject>("credentialConfiguration")
        val formatProperty = if (format == "null") {
            null
        } else {
            format
        }
        credentialIssuer.addProperty("format", formatProperty)
    }

    @When("{actor} adds '{}' schemaId for credential configuration request")
    fun issuerAddsSchemaIdToCredentialConfigurationRequest(issuer: Actor, schema: String) {
        val credentialIssuer = issuer.recall<JsonObject>("credentialConfiguration")
        val schemaIdProperty = if (schema == "null") {
            null
        } else {
            val baseUrl = issuer.recall<String>("baseUrl")
            val schemaGuid = issuer.recall<String>(schema)
            "$baseUrl/schema-registry/schemas/$schemaGuid/schema"
        }
        credentialIssuer.addProperty("schemaId", schemaIdProperty)
    }

    @When("{actor} sends the create a credential configuration request")
    fun issuerSendsTheCredentialConfigurationRequest(issuer: Actor) {
        val credentialConfiguration = issuer.recall<JsonObject>("credentialConfiguration")
        val credentialIssuerId = issuer.recall<String>("credentialConfigurationId").toString()
        issuer.attemptsTo(
            Post.to("/oid4vci/issuers/$credentialIssuerId/credential-configurations").body(credentialConfiguration),
        )
    }

    @Then("{actor} sees the {string} configuration on IssuerMetadata endpoint")
    fun issuerSeesCredentialConfiguration(issuer: Actor, configurationId: String) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers/${credentialIssuer.id}/.well-known/openid-credential-issuer"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
        val metadata = SerenityRest.lastResponse().get<IssuerMetadata>()
        val credConfig = metadata.credentialConfigurationsSupported[configurationId]!!
        issuer.attemptsTo(
            Ensure.that(credConfig.scope).isEqualTo(configurationId),
        )
    }

    @Then("{actor} cannot see the {string} configuration on IssuerMetadata endpoint")
    fun issuerCannotSeeCredentialConfiguration(issuer: Actor, configurationId: String) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers/${credentialIssuer.id}/.well-known/openid-credential-issuer"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
        val metadata = SerenityRest.lastResponse().get<IssuerMetadata>()
        issuer.attemptsTo(
            Ensure.that(metadata.credentialConfigurationsSupported.keys).doesNotContain(configurationId),
        )
    }

    @Then("{actor} should see that create credential configuration has failed with '{}' status code and '{}' detail")
    fun issuerShouldSeeCredentialConfigurationRequestHasFailed(issuer: Actor, statusCode: Int, errorDetail: String) {
        issuer.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(statusCode),
            Ensure.that(SerenityRest.lastResponse().body.asString()).contains(errorDetail),
        )
    }
}
