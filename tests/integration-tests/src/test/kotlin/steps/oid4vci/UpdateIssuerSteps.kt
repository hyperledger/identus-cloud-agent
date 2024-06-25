package steps.oid4vci

import common.CredentialSchema
import interactions.*
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus
import org.hyperledger.identus.client.models.*

class UpdateIssuerSteps {
    private val UPDATE_AUTH_SERVER_URL = "http://example.com"
    private val UPDATE_AUTH_SERVER_CLIENT_ID = "foo"
    private val UPDATE_AUTH_SERVER_CLIENT_SECRET = "bar"

    @When("{actor} updates the oid4vci issuer")
    fun issuerUpdateCredentialIssuer(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Patch.to("/oid4vci/issuers/${credentialIssuer.id}")
                .with {
                    it.body(
                        PatchCredentialIssuerRequest(
                            authorizationServer = PatchAuthorizationServer(
                                url = UPDATE_AUTH_SERVER_URL,
                                clientId = UPDATE_AUTH_SERVER_CLIENT_ID,
                                clientSecret = UPDATE_AUTH_SERVER_CLIENT_SECRET
                            )
                        )
                    )
                },
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK)
        )
    }

    @When("{actor} deletes the oid4vci issuer")
    fun issuerDeleteCredentialIssuer(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Delete("/oid4vci/issuers/${credentialIssuer.id}"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK)
        )
    }

    @Then("{actor} sees the oid4vci issuer updated with new values")
    fun issuerSeesUpdatedCredentialIssuer(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK)
        )
        val updatedIssuer = SerenityRest.lastResponse().get<CredentialIssuerPage>().contents!!
            .find { it.id == credentialIssuer.id }!!
        issuer.attemptsTo(
            Ensure.that(updatedIssuer.authorizationServerUrl).isEqualTo(UPDATE_AUTH_SERVER_URL)
        )
    }

    @Then("{actor} sees the oid4vci IssuerMetadata endpoint updated with new values")
    fun issuerSeesUpdatedCredentialIssuerMetadata(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers/${credentialIssuer.id}/.well-known/openid-credential-issuer"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK)
        )
        val metadata = SerenityRest.lastResponse().get<IssuerMetadata>()
        issuer.attemptsTo(
            Ensure.that(metadata.authorizationServers?.first()!!).isEqualTo(UPDATE_AUTH_SERVER_URL)
        )
    }

    @Then("{actor} cannot see the oid4vci issuer on the agent")
    fun issuerCannotSeeCredentialIssuer(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK)
        )
        val matchedIssuers = SerenityRest.lastResponse().get<CredentialIssuerPage>().contents!!
            .filter { it.id == credentialIssuer.id }
        issuer.attemptsTo(
            Ensure.that(matchedIssuers).isEmpty()
        )
    }

    @Then("{actor} cannot see the oid4vci IssuerMetadata endpoint")
    fun issuerCannotSeeIssuerMetadata(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers/${credentialIssuer.id}/.well-known/openid-credential-issuer"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_NOT_FOUND)
        )
    }

    @When("{actor} uses {} to create a credential configuration {string}")
    fun issuerCreateCredentialConfiguration(issuer: Actor, schema: CredentialSchema, configurationId: String) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        val schemaGuid = issuer.recall<String>(schema.name)
        val baseUrl = issuer.recall<String>("baseUrl")
        issuer.attemptsTo(
            Post.to("/oid4vci/issuers/${credentialIssuer.id}/credential-configurations")
                .with {
                    it.body(
                        CreateCredentialConfigurationRequest(
                            configurationId = configurationId,
                            format = CredentialFormat.JWT_VC_JSON,
                            schemaId = "$baseUrl/schema-registry/schemas/$schemaGuid/schema"
                        )
                    )
                },
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED)
        )
    }

    @Then("{actor} sees the {string} credential configuration on IssuerMetadata endpoint")
    fun issuerSeesCredentialConfiguration(issuer: Actor, configurationId: String) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers/${credentialIssuer.id}/.well-known/openid-credential-issuer"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK)
        )
        val metadata = SerenityRest.lastResponse().get<IssuerMetadata>()
        val credConfig = metadata.credentialConfigurationsSupported[configurationId]!!
        issuer.attemptsTo(
            Ensure.that(credConfig.scope).isEqualTo(configurationId)
        )
    }
}