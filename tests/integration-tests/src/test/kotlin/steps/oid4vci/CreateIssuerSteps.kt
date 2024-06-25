package steps.oid4vci

import interactions.*
import io.cucumber.java.en.*
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK
import org.hyperledger.identus.client.models.*

class CreateIssuerSteps {

    @Given("{actor} has an existing oid4vci issuer")
    fun issuerHasExistingCredentialIssuer(issuer: Actor) {
        issuerCreateCredentialIssuer(issuer)
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
                                clientSecret = issuer.recall("OID4VCI_AUTH_SERVER_CLIENT_SECRET")
                            )
                        )
                    )
                },
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED)
        )
        val credentialIssuer = SerenityRest.lastResponse().get<CredentialIssuer>();
        issuer.remember("oid4vciCredentialIssuer", credentialIssuer)
    }

    @Then("{actor} sees the oid4vci issuer exists on the agent")
    fun issuerSeesCredentialIssuerExists(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK)
        )
        val matchedIssuers = SerenityRest.lastResponse().get<CredentialIssuerPage>().contents!!
            .filter { it.id == credentialIssuer.id }
        issuer.attemptsTo(
            Ensure.that(matchedIssuers).hasSize(1)
        )
    }

    @Then("{actor} sees the oid4vci issuer on IssuerMetadata endpoint")
    fun issuerSeesCredentialIssuerExistsOnMetadataEndpoint(issuer: Actor) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        issuer.attemptsTo(
            Get("/oid4vci/issuers/${credentialIssuer.id}/.well-known/openid-credential-issuer"),
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK)
        )
    }
}
