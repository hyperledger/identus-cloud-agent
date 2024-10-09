package steps.oid4vci

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import interactions.Delete
import interactions.Get
import interactions.Patch
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
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK
import org.hyperledger.identus.client.models.AuthorizationServer
import org.hyperledger.identus.client.models.CreateCredentialIssuerRequest
import org.hyperledger.identus.client.models.CredentialIssuer
import org.hyperledger.identus.client.models.CredentialIssuerPage
import org.hyperledger.identus.client.models.IssuerMetadata
import org.hyperledger.identus.client.models.PatchAuthorizationServer
import org.hyperledger.identus.client.models.PatchCredentialIssuerRequest

class ManageIssuerSteps {
    companion object {
        private const val UPDATE_AUTH_SERVER_URL = "http://example.com"
        private const val UPDATE_AUTH_SERVER_CLIENT_ID = "foo"
        private const val UPDATE_AUTH_SERVER_CLIENT_SECRET = "bar"
    }

    @Given("{actor} has an existing oid4vci issuer")
    fun issuerHasExistingCredentialIssuer(issuer: Actor) {
        if (!issuer.recallAll().containsKey("oid4vciCredentialIssuer")) {
            issuerCreateCredentialIssuer(issuer)
        }
    }

    @When("{actor} creates an oid4vci issuer")
    fun issuerCreateCredentialIssuer(issuer: Actor) {
        issuer.attemptsTo(
            Post.to("/oid4vci/issuers")
                .with {
                    it.body(
                        CreateCredentialIssuerRequest(
                            authorizationServer = AuthorizationServer(
                                url = issuer.recall("OID4VCI_AUTH_SERVER_URL"),
                                clientId = issuer.recall("OID4VCI_AUTH_SERVER_CLIENT_ID"),
                                clientSecret = issuer.recall("OID4VCI_AUTH_SERVER_CLIENT_SECRET"),
                            ),
                        ),
                    )
                },
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )
        val credentialIssuer = SerenityRest.lastResponse().get<CredentialIssuer>()
        issuer.remember("oid4vciCredentialIssuer", credentialIssuer)
    }

    @Then("{actor} sees the oid4vci issuer exists on the agent")
    fun issuerSeesCredentialIssuerExists(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
        val matchedIssuers = SerenityRest.lastResponse().get<CredentialIssuerPage>().contents!!
            .filter { it.id.toString() == credentialIssuer.id }
        issuer.attemptsTo(
            Ensure.that(matchedIssuers).hasSize(1),
        )
    }

    @Then("{actor} sees the oid4vci issuer on IssuerMetadata endpoint")
    fun issuerSeesCredentialIssuerExistsOnMetadataEndpoint(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers/${credentialIssuer.id}/.well-known/openid-credential-issuer"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
    }

    @When("{actor} updates the oid4vci issuer")
    fun issuerUpdateCredentialIssuer(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Patch.to("/oid4vci/issuers/${credentialIssuer.id}").body(
                PatchCredentialIssuerRequest(
                    authorizationServer = PatchAuthorizationServer(
                        url = UPDATE_AUTH_SERVER_URL,
                        clientId = UPDATE_AUTH_SERVER_CLIENT_ID,
                        clientSecret = UPDATE_AUTH_SERVER_CLIENT_SECRET,
                    ),
                ),
            ),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK),
        )
    }

    @When("{actor} tries to update the oid4vci issuer '{}' property using '{}' value")
    fun issuerTriesToUpdateTheOID4VCIIssuer(issuer: Actor, property: String, value: String) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        val body = JsonObject()
        val propertyValue = if (value == "null") { null } else { value }
        body.addProperty(property, propertyValue)

        val gson = GsonBuilder().serializeNulls().create()
        issuer.attemptsTo(
            Patch.to("/oid4vci/issuers/${credentialIssuer.id}").body(gson.toJson(body)),
        )
    }

    @When("{actor} deletes the oid4vci issuer")
    fun issuerDeleteCredentialIssuer(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Delete("/oid4vci/issuers/${credentialIssuer.id}"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK),
        )
    }

    @When("{actor} tries to create oid4vci issuer with '{}', '{}', '{}' and '{}'")
    fun issuerTriesToCreateOIDCIssuer(
        issuer: Actor,
        id: String,
        url: String,
        clientId: String,
        clientSecret: String,
    ) {
        val idProperty = if (id == "null") {
            null
        } else {
            id
        }
        val urlProperty = if (url == "null") {
            null
        } else {
            url
        }
        val clientIdProperty = if (clientId == "null") {
            null
        } else {
            clientId
        }
        val clientSecretProperty = if (clientSecret == "null") {
            null
        } else {
            clientSecret
        }

        val body = JsonObject()
        val authorizationServer = JsonObject()

        body.addProperty("id", idProperty)
        body.add("authorizationServer", authorizationServer)

        authorizationServer.addProperty("url", urlProperty)
        authorizationServer.addProperty("clientId", clientIdProperty)
        authorizationServer.addProperty("clientSecret", clientSecretProperty)

        val gson = GsonBuilder().serializeNulls().create()
        issuer.attemptsTo(
            Post.to("/oid4vci/issuers").body(gson.toJson(body)),
        )
    }

    @Then("{actor} should see the oid4vci '{}' http status response with '{}' detail")
    fun issuerShouldSeeTheOIDC4VCIError(issuer: Actor, httpStatus: Int, errorDetail: String) {
        SerenityRest.lastResponse().body.prettyPrint()
        issuer.attemptsTo(
            Ensure.that(SerenityRest.lastResponse().statusCode).isEqualTo(httpStatus),
            Ensure.that(SerenityRest.lastResponse().body.asString()).contains(errorDetail),
        )
    }

    @Then("{actor} sees the oid4vci issuer updated with new values")
    fun issuerSeesUpdatedCredentialIssuer(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK),
        )
        val updatedIssuer = SerenityRest.lastResponse().get<CredentialIssuerPage>().contents!!
            .find { it.id.toString() == credentialIssuer.id }!!
        issuer.attemptsTo(
            Ensure.that(updatedIssuer.authorizationServerUrl).isEqualTo(UPDATE_AUTH_SERVER_URL),
        )
    }

    @Then("{actor} sees the oid4vci IssuerMetadata endpoint updated with new values")
    fun issuerSeesUpdatedCredentialIssuerMetadata(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers/${credentialIssuer.id}/.well-known/openid-credential-issuer"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK),
        )
        val metadata = SerenityRest.lastResponse().get<IssuerMetadata>()
        issuer.attemptsTo(
            Ensure.that(metadata.authorizationServers?.first()!!).isEqualTo(UPDATE_AUTH_SERVER_URL),
        )
    }

    @Then("{actor} cannot see the oid4vci issuer on the agent")
    fun issuerCannotSeeCredentialIssuer(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_OK),
        )
        val matchedIssuers = SerenityRest.lastResponse().get<CredentialIssuerPage>().contents!!
            .filter { it.id.toString() == credentialIssuer.id }
        issuer.attemptsTo(
            Ensure.that(matchedIssuers).isEmpty(),
        )
    }

    @Then("{actor} cannot see the oid4vci IssuerMetadata endpoint")
    fun issuerCannotSeeIssuerMetadata(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers/${credentialIssuer.id}/.well-known/openid-credential-issuer"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_NOT_FOUND),
        )
    }

    @Then("{actor} should see the update oid4vci issuer returned '{}' http status")
    fun issuerShouldSeeTheUpdateOID4VCIIssuerReturnedHttpStatus(issuer: Actor, statusCode: Int) {
        issuer.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(statusCode),
        )
    }
}
