package steps.oid4vci

import abilities.ListenToEvents
import eu.europa.ec.eudi.openid4vci.*
import interactions.Post
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import kotlinx.coroutines.runBlocking
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus
import org.apache.pdfbox.pdmodel.interactive.annotation.layout.PlainText
import org.htmlunit.*
import org.htmlunit.html.HtmlPage
import org.htmlunit.html.HtmlPasswordInput
import org.htmlunit.html.HtmlSubmitInput
import org.htmlunit.html.HtmlTextInput
import org.hyperledger.identus.client.models.*
import org.hyperledger.identus.client.models.CredentialOfferRequest
import java.net.URI
import java.net.URL

// TODO
// Agent does not support Holder capability.
// These steps should be updated when Agent can act as holder.
class IssueCredentialSteps {
    @When("{actor} creates an offer using {string} configuration with {string} form DID")
    fun issuerCreateCredentialOffer(issuer: Actor, configurationId: String, didForm: String) {
        val credentialIssuer = issuer.recall<CredentialIssuer>("oid4vciCredentialIssuer")
        val claims = linkedMapOf(
            "name" to "Alice",
            "age" to 42,
        )
        val did: String = if (didForm == "short") {
            issuer.recall("shortFormDid")
        } else {
            issuer.recall("longFormDid")
        }
        issuer.attemptsTo(
            Post.to("/oid4vci/issuers/${credentialIssuer.id}/credential-offers")
                .with {
                    it.body(
                        CredentialOfferRequest(
                            credentialConfigurationId = configurationId,
                            issuingDID = did,
                            claims = claims
                        )
                    )
                },
            Ensure.thatTheLastResponse().statusCode().isEqualTo(HttpStatus.SC_CREATED),
        )
        val offerUri = SerenityRest.lastResponse().get<CredentialOfferResponse>().credentialOffer
        issuer.remember("oid4vciOffer", offerUri)
    }

    @When("{actor} receives oid4vci offer from {actor}")
    fun holderReceivesOfferFromIssuer(holder: Actor, issuer: Actor) {
        val offerUri = issuer.recall<String>("oid4vciOffer")
        holder.remember("oid4vciOffer", offerUri)
    }

    @When("{actor} resolves oid4vci issuer metadata and login via front-end channel")
    fun holderResolvesIssuerMetadata(holder: Actor) {
        val offerUri = holder.recall<String>("oid4vciOffer")
        val credentialOffer = runBlocking {
            CredentialOfferRequestResolver().resolve(offerUri).getOrThrow()
        }
        val redirectUrl = holder.recall<URL>("webhookUrl")
        val openId4VCIConfig = OpenId4VCIConfig(
            clientId = holder.recall("OID4VCI_AUTH_SERVER_CLIENT_ID"),
            authFlowRedirectionURI = URI.create("${redirectUrl}/auth-cb"),
            keyGenerationConfig = KeyGenerationConfig.ecOnly(com.nimbusds.jose.jwk.Curve.SECP256K1),
            credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED,
            parUsage = ParUsage.Never
        )
        val issuer = Issuer.make(openId4VCIConfig, credentialOffer).getOrThrow()
        val authorizationRequest = runBlocking {
            issuer.prepareAuthorizationRequest().getOrThrow()
        }
        val authCode = keycloakLoginViaBrowser(authorizationRequest.authorizationCodeURL.value, holder)
        holder.remember("authorizationCode", authCode)
    }

    private fun keycloakLoginViaBrowser(loginUrl: URL, actor: Actor): String {
        val client = WebClient()
        val loginPage = client.getPage<HtmlPage>(loginUrl)
        val loginForm = loginPage.forms.first()
        loginForm.getInputByName<HtmlTextInput>("username").type(actor.name)
        loginForm.getInputByName<HtmlPasswordInput>("password").type(actor.name)
        val consentPage = loginForm.getInputByName<HtmlSubmitInput>("login").click<HtmlPage>()
        val consentForm = consentPage.forms.first()
        consentForm.getInputByName<HtmlSubmitInput>("accept").click<TextPage>()
        client.close()
        return ListenToEvents.with(actor).authCodeCallbackEvents.last()
    }
}