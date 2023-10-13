package features

import com.sksamuel.hoplite.ConfigLoader
import common.ListenToEvents
import config.Config
import features.connection.ConnectionSteps
import features.credentials.IssueCredentialsSteps
import features.did.PublishDidSteps
import features.multitenancy.EventsSteps
import interactions.Get
import io.cucumber.java.AfterAll
import io.cucumber.java.BeforeAll
import io.cucumber.java.ParameterType
import io.cucumber.java.en.Given
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import io.iohk.atala.prism.models.*
import io.restassured.RestAssured
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.actors.Cast
import net.serenitybdd.screenplay.actors.OnStage
import net.serenitybdd.screenplay.rest.abilities.CallAnApi
import org.apache.http.HttpStatus
import org.apache.http.HttpStatus.SC_OK
import java.util.*
import kotlin.random.Random

@OptIn(ExperimentalStdlibApi::class)
@BeforeAll
fun initializeIssuerVerifierMultitenantAgent() {
    val eventSteps = EventsSteps()
    val cast = Cast()

    val config = ConfigLoader().loadConfigOrThrow<Config>("/tests.conf")

    cast.actorNamed("Admin", CallAnApi.at(config.admin.url.toExternalForm()))
    cast.actorNamed(
        "Acme",
        CallAnApi.at(config.issuer.url.toExternalForm()),
        ListenToEvents.at(config.issuer.webhookUrl!!)
    )
    cast.actorNamed(
        "Bob",
        CallAnApi.at(config.holder.url.toExternalForm()),
        ListenToEvents.at(config.holder.webhookUrl!!)
    )
    cast.actorNamed(
        "Faber",
        CallAnApi.at(config.verifier.url.toExternalForm()),
        ListenToEvents.at(config.verifier.webhookUrl!!)
    )
    OnStage.setTheStage(cast)

    // Create issuer wallet and tenant
    val createIssuerWalletResponse = RestAssured
        .given().body(
            CreateWalletRequest(
                name = "IssuerWallet",
                seed = Random.nextBytes(64).toHexString(),
                id = UUID.randomUUID()
            )
        )
        .header(config.global.adminAuthHeader, config.admin.apikey)
        .post("${config.issuer.url}/wallets")
        .thenReturn()
    Ensure.that(createIssuerWalletResponse.statusCode).isEqualTo(HttpStatus.SC_CREATED)
    val issuerWallet = createIssuerWalletResponse.body.jsonPath().getObject("", WalletDetail::class.java)

    val issuerTenantResponse = RestAssured
        .given().body(
            CreateEntityRequest(
                name = "Issuer",
                walletId = issuerWallet.id
            )
        )
        .header(config.global.adminAuthHeader, config.admin.apikey)
        .post("${config.issuer.url}/iam/entities")
        .thenReturn()
    Ensure.that(issuerTenantResponse.statusCode).isEqualTo(HttpStatus.SC_CREATED)
    val issuerEntity = issuerTenantResponse.body.jsonPath().getObject("", EntityResponse::class.java)

    val issuerAddApiKeyResponse =
        RestAssured
            .given().body(
                ApiKeyAuthenticationRequest(
                    entityId = issuerEntity.id,
                    apiKey = config.issuer.apikey!!
                )
            )
            .header(config.global.adminAuthHeader, config.admin.apikey)
            .post("${config.issuer.url}/iam/apikey-authentication")
            .thenReturn()
    Ensure.that(issuerAddApiKeyResponse.statusCode).isEqualTo(HttpStatus.SC_CREATED)

    // Create verifier wallet
    val createVerifierWalletResponse = RestAssured
        .given().body(
            CreateWalletRequest(
                name = "VerifierWallet",
                seed = Random.nextBytes(64).toHexString(),
                id = UUID.randomUUID()
            )
        )
        .header(config.global.adminAuthHeader, config.admin.apikey)
        .post("${config.verifier.url}/wallets")
        .thenReturn()
    Ensure.that(createVerifierWalletResponse.statusCode).isEqualTo(HttpStatus.SC_CREATED)
    val verifierWallet = createVerifierWalletResponse.body.jsonPath().getObject("", WalletDetail::class.java)
    // Create verifier tenant
    val verifierTenantResponse = RestAssured
        .given().body(
            CreateEntityRequest(
                name = "Verifier",
                walletId = verifierWallet.id
            )
        )
        .header(config.global.adminAuthHeader, config.admin.apikey)
        .post("${config.verifier.url}/iam/entities")
        .thenReturn()
    Ensure.that(verifierTenantResponse.statusCode).isEqualTo(HttpStatus.SC_CREATED)
    val verifierEntity = verifierTenantResponse.body.jsonPath().getObject("", EntityResponse::class.java)
    // Add verifier api key
    val verifierAddApiKeyResponse =
        RestAssured
            .given().body(
                ApiKeyAuthenticationRequest(
                    entityId = verifierEntity.id,
                    apiKey = config.verifier.apikey!!
                )
            )
            .header(config.global.adminAuthHeader, config.admin.apikey)
            .post("${config.verifier.url}/iam/apikey-authentication")
            .thenReturn()
    Ensure.that(verifierAddApiKeyResponse.statusCode).isEqualTo(HttpStatus.SC_CREATED)

    cast.actors.forEach { actor ->
        when (actor.name) {
            "Acme" -> {
                actor.remember("AUTH_KEY", config.issuer.apikey)
            }
            "Bob" -> {
                actor.remember("AUTH_KEY", config.holder.apikey)
            }
            "Faber" -> {
                actor.remember("AUTH_KEY", config.verifier.apikey)
            }
        }
    }

    val registerIssuerWebhookResponse =
        RestAssured
            .given().body(
                CreateWebhookNotification(
                    url = config.issuer.webhookUrl.toExternalForm()
                )
            )
            .header(config.global.authHeader, config.issuer.apikey)
            .post("${config.issuer.url}/events/webhooks")
            .thenReturn()
    Ensure.that(registerIssuerWebhookResponse.statusCode).isEqualTo(HttpStatus.SC_CREATED)

    val registerVerifierWebhookResponse =
        RestAssured
            .given().body(
                CreateWebhookNotification(
                    url = config.verifier.webhookUrl.toExternalForm()
                )
            )
            .header(config.global.authHeader, config.verifier.apikey)
            .post("${config.verifier.url}/events/webhooks")
            .thenReturn()
    Ensure.that(registerVerifierWebhookResponse.statusCode).isEqualTo(HttpStatus.SC_CREATED)
}

@AfterAll
fun clearStage() {
    OnStage.drawTheCurtain()
}

class CommonSteps {
    @ParameterType(".*")
    fun actor(actorName: String): Actor {
        return OnStage.theActorCalled(actorName)
    }

    @Given("{actor} has an issued credential from {actor}")
    fun holderHasIssuedCredentialFromIssuer(holder: Actor, issuer: Actor) {
        holder.attemptsTo(
            Get.resource("/issue-credentials/records")
        )
        holder.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK)
        )
        val receivedCredential = SerenityRest.lastResponse().get<IssueCredentialRecordPage>().contents!!.findLast { credential ->
            credential.protocolState == IssueCredentialRecord.ProtocolState.credentialReceived
        }

        if (receivedCredential != null) {
            holder.remember("issuedCredential", receivedCredential)
        } else {
            val publishDidSteps = PublishDidSteps()
            val issueSteps = IssueCredentialsSteps()
            actorsHaveExistingConnection(issuer, holder)
            publishDidSteps.createsUnpublishedDid(holder)
            publishDidSteps.createsUnpublishedDid(issuer)
            publishDidSteps.hePublishesDidToLedger(issuer)
            issueSteps.acmeOffersACredential(issuer, holder, "short")
            issueSteps.bobRequestsTheCredential(holder)
            issueSteps.acmeIssuesTheCredential(issuer)
            issueSteps.bobHasTheCredentialIssued(holder)
        }
    }

    @Given("{actor} and {actor} have an existing connection")
    fun actorsHaveExistingConnection(inviter: Actor, invitee: Actor) {
        inviter.attemptsTo(
            Get.resource("/connections")
        )
        inviter.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK)
        )
        val inviterConnection = SerenityRest.lastResponse().get<ConnectionsPage>().contents!!.firstOrNull {
            it.label == "Connection with ${invitee.name}" && it.state == Connection.State.connectionResponseSent
        }

        var inviteeConnection: Connection? = null
        if (inviterConnection != null) {
            invitee.attemptsTo(
                Get.resource("/connections")
            )
            invitee.attemptsTo(
                Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_OK)
            )
            inviteeConnection = SerenityRest.lastResponse().get<ConnectionsPage>().contents!!.firstOrNull {
                it.theirDid == inviterConnection.myDid && it.state == Connection.State.connectionResponseReceived
            }
        }

        if (inviterConnection != null && inviteeConnection != null) {
            inviter.remember("connection-with-${invitee.name}", inviterConnection)
            invitee.remember("connection-with-${inviter.name}", inviteeConnection)
        } else {
            val connectionSteps = ConnectionSteps()
            connectionSteps.inviterGeneratesAConnectionInvitation(inviter, invitee)
            connectionSteps.inviteeSendsAConnectionRequestToInviter(invitee, inviter)
            connectionSteps.inviterReceivesTheConnectionRequest(inviter)
            connectionSteps.inviteeReceivesTheConnectionResponse(invitee)
            connectionSteps.inviterAndInviteeHaveAConnection(inviter, invitee)
        }
    }
}
