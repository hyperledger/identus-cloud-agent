package steps.oid4vci

import abilities.ListenToEvents
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWK
import eu.europa.ec.eudi.openid4vci.*
import interactions.Post
import interactions.body
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import kotlinx.coroutines.runBlocking
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus
import org.htmlunit.*
import org.htmlunit.html.HtmlPage
import org.htmlunit.html.HtmlPasswordInput
import org.htmlunit.html.HtmlSubmitInput
import org.htmlunit.html.HtmlTextInput
import org.hyperledger.identus.apollo.base64.base64UrlEncoded
import org.hyperledger.identus.apollo.utils.KMMECSecp256k1PrivateKey
import org.hyperledger.identus.apollo.utils.decodeHex
import org.hyperledger.identus.client.models.*
import org.hyperledger.identus.client.models.CredentialOfferRequest
import java.net.URI
import java.net.URL

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
            Post.to("/oid4vci/issuers/${credentialIssuer.id}/credential-offers").body(
                CredentialOfferRequest(
                    credentialConfigurationId = configurationId,
                    issuingDID = did,
                    claims = claims,
                ),
            ),
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
            authFlowRedirectionURI = URI.create("$redirectUrl/auth-cb"),
            keyGenerationConfig = KeyGenerationConfig.ecOnly(com.nimbusds.jose.jwk.Curve.SECP256K1),
            credentialResponseEncryptionPolicy = CredentialResponseEncryptionPolicy.SUPPORTED,
            parUsage = ParUsage.Never,
        )
        val issuer = Issuer.make(openId4VCIConfig, credentialOffer).getOrThrow()
        val authorizationRequest = runBlocking {
            issuer.prepareAuthorizationRequest().getOrThrow()
        }
        val authResponse = keycloakLoginViaBrowser(authorizationRequest.authorizationCodeURL.value, holder)
        val authorizedRequest =
            with(issuer) {
                runBlocking {
                    val authCode = AuthorizationCode(authResponse.first)
                    authorizationRequest.authorizeWithAuthorizationCode(authCode, authResponse.second).getOrThrow()
                }
            }
        holder.remember("eudiCredentialOffer", credentialOffer)
        holder.remember("eudiAuthorizedRequest", authorizedRequest)
        holder.remember("eudiIssuer", issuer)
    }

    @When("{actor} presents the access token with JWT proof on CredentialEndpoint")
    fun holderPresentsTokenOnCredentialEdpoint(holder: Actor) {
        val credentialOffer = holder.recall<CredentialOffer>("eudiCredentialOffer")
        val issuer = holder.recall<Issuer>("eudiIssuer")
        val authorizedRequest = holder.recall<AuthorizedRequest>("eudiAuthorizedRequest")
        val requestPayload = IssuanceRequestPayload.ConfigurationBased(credentialOffer.credentialConfigurationIdentifiers.first(), null)
        val authRequestAndsubmissionOutcome = with(issuer) {
            when (authorizedRequest) {
                is AuthorizedRequest.NoProofRequired -> throw Exception("Not supported yet")
                is AuthorizedRequest.ProofRequired -> runBlocking {
                    authorizedRequest.requestSingle(
                        requestPayload,
                        popSigner(),
                    )
                }
            }.getOrThrow()
        }
        holder.remember("eudiSubmissionOutcome", authRequestAndsubmissionOutcome.second)
    }

    @Then("{actor} sees credential issued successfully from CredentialEndpoint")
    fun holderSeesCredentialIssuedSuccessfully(holder: Actor) {
        val credentials = when (val submissionOutcome = holder.recall<SubmissionOutcome>("eudiSubmissionOutcome")) {
            is SubmissionOutcome.Success -> submissionOutcome.credentials
            else -> throw Exception("Issuance failed. $submissionOutcome")
        }
        holder.attemptsTo(
            Ensure.that(credentials).hasSize(1),
        )
    }

    private fun popSigner(): PopSigner {
        val privateKeyHex = "d93c6485e30aad4d6522313553e58d235693f7007b822676e5e1e9a667655b69"
        val did = "did:prism:4a2bc09be65136f604d1564e2fced1a1cdbce9deb9b64ee396afc95fc0b01c59:CnsKeRI6CgZhdXRoLTEQBEouCglzZWNwMjU2azESIQOx16yykO2nDcmM-NeQeVipxmuaF38KasIA8gycJCHWJhI7CgdtYXN0ZXIwEAFKLgoJc2VjcDI1NmsxEiECKrfbf1_p7YT5aRJspBLct5zDyL6aicEam1Gycq5xKy0"
        val kid = "$did#auth-1"
        val privateKey = KMMECSecp256k1PrivateKey.secp256k1FromByteArray(privateKeyHex.decodeHex())
        val point = privateKey.getPublicKey().getCurvePoint()
        val jwk = JWK.parse(
            mapOf(
                "kty" to "EC",
                "crv" to "secp256k1",
                "x" to point.x.base64UrlEncoded,
                "y" to point.y.base64UrlEncoded,
                "d" to privateKey.raw.base64UrlEncoded,
            ),
        )
        return PopSigner.jwtPopSigner(
            privateKey = jwk,
            algorithm = JWSAlgorithm.ES256K,
            publicKey = JwtBindingKey.Did(identity = kid),
        )
    }

    /**
     * @return A tuple of authorization code and authorization server state
     */
    private fun keycloakLoginViaBrowser(loginUrl: URL, actor: Actor): Pair<String, String> {
        val client = WebClient()

        // step 1 - login with username and password
        val loginPage = client.getPage<HtmlPage>(loginUrl)
        val loginForm = loginPage.forms.first()
        loginForm.getInputByName<HtmlTextInput>("username").type(actor.name)
        loginForm.getInputByName<HtmlPasswordInput>("password").type(actor.name)
        val postLoginPage = loginForm.getInputByName<HtmlSubmitInput>("login").click<Page>()

        // If it is the first time user is logged in, Keycloak ask for consent by returning HtmlPage.
        if (postLoginPage is HtmlPage) {
            // step 2 - give client consent to access the scopes
            val consentForm = postLoginPage.forms.first()
            consentForm.getInputByName<HtmlSubmitInput>("accept").click<TextPage>()
        }

        client.close()
        return ListenToEvents.with(actor).authCodeCallbackEvents.last()
    }
}
