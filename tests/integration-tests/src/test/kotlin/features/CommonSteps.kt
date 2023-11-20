package features

import com.sksamuel.hoplite.ConfigLoader
import common.ListenToEvents
import config.*
import features.connection.ConnectionSteps
import features.credentials.IssueCredentialsSteps
import features.did.PublishDidSteps
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
import org.apache.http.HttpStatus.SC_CREATED
import org.apache.http.HttpStatus.SC_OK
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.util.*

val environments: MutableList<ComposeContainer> = mutableListOf()

fun initializeVdr(prismNode: PrismNodeConf) {
    val vdrEnvironment: ComposeContainer = ComposeContainer(
        File("src/test/resources/containers/vdr.yml")
    ).withEnv(
        mapOf(
            "PRISM_NODE_VERSION" to prismNode.version,
            "PRISM_NODE_PORT" to prismNode.httpPort.toString()
        )
    ).waitingFor(
        "prism-node", Wait.forLogMessage(".*Server started, listening on.*", 1)
    )
    environments.add(vdrEnvironment)
    vdrEnvironment.start()
}

fun initializeKeycloak(keycloakConf: KeycloakConf) {
    val keycloakEnvironment: ComposeContainer = ComposeContainer(
        File("src/test/resources/containers/keycloak.yml")
    ).withEnv(
        mapOf(
            "KEYCLOAK_HTTP_PORT" to keycloakConf.httpPort.toString(),
        )
    ).waitingFor(
        "keycloak", Wait.forLogMessage(".*Running the server.*", 1)
    )
    environments.add(keycloakEnvironment)
    keycloakEnvironment.start()

    // Get admin token
    val getAdminTokenResponse =
        RestAssured
            .given().body("grant_type=password&client_id=admin-cli&username=admin&password=admin")
            .contentType("application/x-www-form-urlencoded")
            .post("http://localhost:${keycloakConf.httpPort}/realms/master/protocol/openid-connect/token")
            .thenReturn()
    getAdminTokenResponse.then().statusCode(SC_OK)
    val adminToken = getAdminTokenResponse.body.jsonPath().getString("access_token")

    // Create realm
    val createRealmResponse =
        RestAssured
            .given().body(
                mapOf(
                    "realm" to keycloakConf.realm,
                    "enabled" to true,
                    "accessTokenLifespan" to 3600000
                )
            )
            .header("Authorization", "Bearer $adminToken")
            .contentType("application/json")
            .post("http://localhost:${keycloakConf.httpPort}/admin/realms")
            .then().statusCode(SC_CREATED)

    // Create client
    val createClientResponse =
        RestAssured
            .given().body(
                mapOf(
                    "id" to keycloakConf.clientId,
                    "directAccessGrantsEnabled" to true,
                    "authorizationServicesEnabled" to true,
                    "serviceAccountsEnabled" to true,
                    "secret" to keycloakConf.clientSecret,
                ))
            .header("Authorization", "Bearer $adminToken")
            .contentType("application/json")
            .post("http://localhost:${keycloakConf.httpPort}/admin/realms/${keycloakConf.realm}/clients")
            .then().statusCode(SC_CREATED)

    // Create users
    keycloakConf.users.forEach { keycloakUser ->
            RestAssured
                .given().body(
                    mapOf(
                        "id" to keycloakUser.username,
                        "username" to keycloakUser.username,
                        "firstName" to keycloakUser.username,
                        "enabled" to true,
                        "credentials" to listOf(
                            mapOf(
                                "value" to keycloakUser.password,
                                "temporary" to false
                            )
                        )
                    )
                )
                .header("Authorization", "Bearer $adminToken")
                .contentType("application/json")
                .post("http://localhost:${keycloakConf.httpPort}/admin/realms/${keycloakConf.realm}/users")
                .then().statusCode(SC_CREATED)
    }
}

fun initializeAgent(agentInitConf: AgentInitConf) {
    val config = ConfigLoader().loadConfigOrThrow<Config>(System.getenv("INTEGRATION_TESTS_CONFIG") ?: "/configs/basic.conf")
    val agentConfMap: Map<String, String> = mapOf(
        "OPEN_ENTERPRISE_AGENT_VERSION" to agentInitConf.version,
        "API_KEY_ENABLED" to agentInitConf.authEnabled.toString(),
        "AUTH_HEADER" to config.global.authHeader,
        "ADMIN_AUTH_HEADER" to config.global.adminAuthHeader,
        "AGENT_DIDCOMM_PORT" to agentInitConf.didcommPort.toString(),
        "AGENT_HTTP_PORT" to agentInitConf.httpPort.toString(),
        "PRISM_NODE_PORT" to if (config.services.prismNode != null)
            config.services.prismNode.httpPort.toString() else "",
        "SECRET_STORAGE_BACKEND" to agentInitConf.secretStorageBackend,
        "VAULT_HTTP_PORT" to if (config.services.vault != null && agentInitConf.secretStorageBackend == "vault")
            config.services.vault.httpPort.toString() else "",
        "KEYCLOAK_ENABLED" to agentInitConf.keycloakEnabled.toString(),
        "KEYCLOAK_HTTP_PORT" to if (config.services.keycloak != null && agentInitConf.keycloakEnabled)
            config.services.keycloak.httpPort.toString() else "",
        "KEYCLOAK_REALM" to if (config.services.keycloak != null && agentInitConf.keycloakEnabled)
            config.services.keycloak.realm else "",
        "KEYCLOAK_CLIENT_ID" to if (config.services.keycloak != null && agentInitConf.keycloakEnabled)
            config.services.keycloak.clientId else "",
        "KEYCLOAK_CLIENT_SECRET" to if (config.services.keycloak != null && agentInitConf.keycloakEnabled)
            config.services.keycloak.clientSecret else "",
    )
    val environment: ComposeContainer = ComposeContainer(
        File("src/test/resources/containers/agent.yml")
    ).withEnv(agentConfMap).waitingFor("open-enterprise-agent", Wait.forHealthcheck())
    environments.add(environment)
    environment.start()
}

fun initializeWallet(agentConf: AgentConf,  bearerToken: String? = "") {
    val config = ConfigLoader().loadConfigOrThrow<Config>(System.getenv("INTEGRATION_TESTS_CONFIG") ?: "/configs/basic.conf")
    val createWalletResponse =
        RestAssured
            .given().body(
                CreateWalletRequest(
                    name = UUID.randomUUID().toString()
                )
            )
            .header("Authorization", "Bearer $bearerToken")
            .post("${agentConf.url}/wallets")
            .then().statusCode(HttpStatus.SC_CREATED)
}

fun initializeWebhook(agentConf: AgentConf, bearerToken: String? = "") {
    val config = ConfigLoader().loadConfigOrThrow<Config>(System.getenv("INTEGRATION_TESTS_CONFIG") ?: "/configs/basic.conf")
    val registerWebhookResponse =
        RestAssured
            .given().body(
                CreateWebhookNotification(
                    url = agentConf.webhookUrl!!.toExternalForm()
                )
            )
            .header("Authorization", "Bearer $bearerToken")
            .header(config.global.authHeader, agentConf.apikey)
            .post("${agentConf.url}/events/webhooks")
            .then().statusCode(HttpStatus.SC_OK)
}

fun getKeycloakAuthToken(keycloakConf: KeycloakConf, username: String, password: String): String {
    val tokenResponse =
        RestAssured
            .given().body("grant_type=password&client_id=${keycloakConf.clientId}&client_secret=${keycloakConf.clientSecret}&username=${username}&password=${password}")
            .contentType("application/x-www-form-urlencoded")
            .header("Host", "localhost")
            .post("http://localhost:${keycloakConf.httpPort}/realms/${keycloakConf.realm}/protocol/openid-connect/token")
            .thenReturn()
    tokenResponse.then().statusCode(HttpStatus.SC_OK)
    return tokenResponse.body.jsonPath().getString("access_token")
}

@BeforeAll
fun initAgents() {
    val cast = Cast()
    val config = ConfigLoader().loadConfigOrThrow<Config>(System.getenv("INTEGRATION_TESTS_CONFIG") ?: "/configs/basic.conf")
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
    cast.actorNamed(
        "Admin",
        CallAnApi.at(config.admin.url.toExternalForm())
    )
    OnStage.setTheStage(cast)

    if (config.services.keycloak != null) {
        initializeKeycloak(config.services.keycloak)
    }

    if (config.services.prismNode != null) {
        initializeVdr(config.services.prismNode)
    }
    // Initialize the agents
    config.agents.forEach { agent ->
        initializeAgent(agent)
    }

    if (config.services.keycloak != null) {
        cast.actors.forEach { actor ->
            actor.remember("KEYCLOAK_BEARER_TOKEN", getKeycloakAuthToken(config.services.keycloak, actor.name, actor.name))
            when (actor.name) {
                "Acme" -> {
                    initializeWallet(config.issuer, cast.actorNamed(actor.name).recall<String>("KEYCLOAK_BEARER_TOKEN"))
                }
                "Bob" -> {
                    initializeWallet(config.holder, cast.actorNamed(actor.name).recall<String>("KEYCLOAK_BEARER_TOKEN"))
                }
                "Faber" -> {
                    initializeWallet(config.verifier, cast.actorNamed(actor.name).recall<String>("KEYCLOAK_BEARER_TOKEN"))
                }
            }
        }
    }

    initializeWebhook(config.issuer, cast.actorNamed("Acme").recall<String>("KEYCLOAK_BEARER_TOKEN"))
    initializeWebhook(config.holder, cast.actorNamed("Bob").recall<String>("KEYCLOAK_BEARER_TOKEN"))
    initializeWebhook(config.verifier, cast.actorNamed("Faber").recall<String>("KEYCLOAK_BEARER_TOKEN"))

    cast.actors.forEach { actor ->
        when (actor.name) {
            "Acme" -> {
                actor.remember("AUTH_KEY", config.issuer.apikey)
                actor.remember("AUTH_HEADER", config.global.authHeader)
            }
            "Bob" -> {
                actor.remember("AUTH_KEY", config.holder.apikey)
                actor.remember("AUTH_HEADER", config.global.authHeader)
            }
            "Faber" -> {
                actor.remember("AUTH_KEY", config.verifier.apikey)
                actor.remember("AUTH_HEADER", config.global.authHeader)
            }
            "Admin" -> {
                actor.remember("AUTH_KEY", config.admin.apikey)
                actor.remember("AUTH_HEADER", config.global.adminAuthHeader)
            }
        }
    }
}

@AfterAll
fun clearStage() {
    OnStage.drawTheCurtain()
    environments.forEach { environment ->
        environment.stop()
    }
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
            credential.protocolState == IssueCredentialRecord.ProtocolState.CREDENTIAL_RECEIVED
                    && credential.credentialFormat == IssueCredentialRecord.CredentialFormat.JWT
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
            issueSteps.holderReceivesCredentialOffer(holder)
            issueSteps.holderAcceptsCredentialOfferForJwt(holder)
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
            it.label == "Connection with ${invitee.name}" && it.state == Connection.State.CONNECTION_RESPONSE_SENT
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
                it.theirDid == inviterConnection.myDid && it.state == Connection.State.CONNECTION_RESPONSE_RECEIVED
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
