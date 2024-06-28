package steps.oid4vci

import common.CredentialSchema
import interactions.*
import io.cucumber.java.en.*
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus
import org.hyperledger.identus.client.models.*

class ManageCredentialConfigSteps {
    @Given("{actor} has {string} credential configuration created from {}")
    fun issuerHasExistingCredentialConfig(issuer: Actor, configurationId: String, schema: CredentialSchema) {
        ManageIssuerSteps().issuerHasExistingCredentialIssuer(issuer)
        issuerCreateCredentialConfiguration(issuer, schema, configurationId)
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
                            schemaId = "$baseUrl/schema-registry/schemas/$schemaGuid/schema",
                        ),
                    )
                },
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED),
        )
    }

    @When("{actor} deletes {string} credential configuration")
    fun issuerDeletesCredentialConfiguration(issuer: Actor, configurationId: String) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Delete("/oid4vci/issuers/${credentialIssuer.id}/credential-configurations/$configurationId"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK),
        )
    }

    @Then("{actor} sees the {string} configuration on IssuerMetadata endpoint")
    fun issuerSeesCredentialConfiguration(issuer: Actor, configurationId: String) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers/${credentialIssuer.id}/.well-known/openid-credential-issuer"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK),
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
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK),
        )
        val metadata = SerenityRest.lastResponse().get<IssuerMetadata>()
        issuer.attemptsTo(
            Ensure.that(metadata.credentialConfigurationsSupported.keys).doesNotContain(configurationId),
        )
    }
}
