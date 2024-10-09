package config.services

import com.sksamuel.hoplite.ConfigAlias
import config.AgentRole
import config.Role
import io.iohk.atala.automation.utils.Logger
import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.specification.RequestSpecification
import org.apache.http.HttpStatus
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File

data class Keycloak(
    @ConfigAlias("http_port") val httpPort: Int,
    val realm: String = "atala-demo",
    @ConfigAlias("client_id") val clientId: String = "cloud-agent",
    @ConfigAlias("client_secret") val clientSecret: String = "cloud-agent-secret",
    @ConfigAlias("compose_file") val keycloakComposeFile: String = "src/test/resources/containers/keycloak.yml",
    @ConfigAlias("logger_name") val loggerName: String = "keycloak",
    @ConfigAlias("extra_envs") val extraEnvs: Map<String, String> = emptyMap(),
    @ConfigAlias("extra_clients") val extraClients: Map<String, KeycloakPublicClientConfig> = emptyMap(),
    @ConfigAlias("extra_scopes") val extraScopes: List<String> = emptyList(),
) : ServiceBase() {
    private val logger = Logger.get<Keycloak>()
    private val keycloakEnvConfig: Map<String, String> = extraEnvs + mapOf(
        "KEYCLOAK_HTTP_PORT" to httpPort.toString(),
    )
    override val logServices: List<String> = listOf(loggerName)
    private val keycloakClientRoles: List<String> = AgentRole.entries.map { it.roleName }
    override val container: ComposeContainer =
        ComposeContainer(File(keycloakComposeFile)).withEnv(keycloakEnvConfig)
            .waitingFor("keycloak", Wait.forLogMessage(".*Running the server.*", 1))
    private val keycloakBaseUrl = "http://localhost:$httpPort/"
    private var requestBuilder: RequestSpecification? = null
    private var users: List<Role> = emptyList()

    fun setUsers(users: List<Role>): ServiceBase {
        this.users = users
        return this
    }

    override fun postStart() {
        logger.info("Setting up Keycloak")
        initRequestBuilder()
        createRealm()
        createAgentClient()
        createPublicClients()
        createClientRoles()
        createScopes()
        createUsers(users)
    }

    fun getKeycloakAuthToken(username: String, password: String): String {
        val tokenResponse =
            RestAssured
                .given()
                .body("grant_type=password&client_id=$clientId&client_secret=$clientSecret&username=$username&password=$password")
                .contentType("application/x-www-form-urlencoded")
                .header("Host", "localhost")
                .post("http://localhost:$httpPort/realms/$realm/protocol/openid-connect/token")
                .thenReturn()
        tokenResponse.then().statusCode(HttpStatus.SC_OK)
        return tokenResponse.body.jsonPath().getString("access_token")
    }

    private fun getAdminToken(): String {
        val getAdminTokenResponse =
            RestAssured.given().body("grant_type=password&client_id=admin-cli&username=admin&password=admin")
                .contentType("application/x-www-form-urlencoded")
                .baseUri(keycloakBaseUrl)
                .post("/realms/master/protocol/openid-connect/token")
                .thenReturn()
        getAdminTokenResponse.then().statusCode(HttpStatus.SC_OK)
        return getAdminTokenResponse.body.jsonPath().getString("access_token")
    }

    private fun initRequestBuilder() {
        requestBuilder = RequestSpecBuilder()
            .setBaseUri(keycloakBaseUrl)
            .setContentType("application/json")
            .addHeader("Authorization", "Bearer ${getAdminToken()}")
            .build()
    }

    private fun createRealm() {
        RestAssured.given().spec(requestBuilder)
            .body(
                mapOf(
                    "realm" to realm,
                    "enabled" to true,
                    "accessTokenLifespan" to 3600000,
                ),
            )
            .post("/admin/realms")
            .then().statusCode(HttpStatus.SC_CREATED)
    }

    private fun createAgentClient() {
        RestAssured.given().spec(requestBuilder)
            .body(
                mapOf(
                    "id" to clientId,
                    "directAccessGrantsEnabled" to true,
                    "authorizationServicesEnabled" to true,
                    "serviceAccountsEnabled" to true,
                    "secret" to clientSecret,
                ),
            )
            .post("/admin/realms/$realm/clients")
            .then().statusCode(HttpStatus.SC_CREATED)
    }

    private fun createPublicClients() {
        extraClients.forEach { client ->
            RestAssured.given().spec(requestBuilder)
                .body(
                    mapOf(
                        "id" to client.key,
                        "publicClient" to true,
                        "consentRequired" to true,
                        "redirectUris" to client.value.redirectUris,
                    ),
                )
                .post("/admin/realms/$realm/clients")
                .then().statusCode(HttpStatus.SC_CREATED)
        }
    }

    private fun createScopes() {
        extraScopes.forEach { scope ->
            val response = RestAssured.given().spec(requestBuilder)
                .body(
                    mapOf(
                        "name" to scope,
                        "description" to scope,
                        "protocol" to "openid-connect",
                        "attributes" to mapOf(
                            "consent.screen.text" to scope,
                            "display.on.consent.screen" to true,
                            "include.in.token.scope" to true,
                            "gui.order" to "",
                        ),
                    ),
                )
                .post("/admin/realms/$realm/client-scopes")
                .thenReturn()
            response.then().statusCode(HttpStatus.SC_CREATED)
            val clientScopeId = response.getHeader("Location").split("/").last()
            mapClientsScopeToClient(clientScopeId)
        }
    }

    private fun mapClientsScopeToClient(clientScopeId: String) {
        extraClients.keys.forEach { client ->
            RestAssured.given().spec(requestBuilder)
                .put("/admin/realms/$realm/clients/$client/optional-client-scopes/$clientScopeId")
                .then()
                .statusCode(HttpStatus.SC_NO_CONTENT)
        }
    }

    private fun createClientRoles() {
        keycloakClientRoles.forEach { roleName ->
            RestAssured.given().spec(requestBuilder)
                .body(
                    mapOf(
                        "name" to roleName,
                    ),
                )
                .post("/admin/realms/$realm/clients/$clientId/roles")
                .then().statusCode(HttpStatus.SC_CREATED)
        }
    }

    private fun createUsers(users: List<Role>) {
        users.forEach { role ->
            val keycloakUser = role.name
            val createUserResponse = RestAssured.given().spec(requestBuilder)
                .body(
                    mapOf(
                        "id" to keycloakUser,
                        "username" to keycloakUser,
                        "firstName" to keycloakUser,
                        "enabled" to true,
                        "credentials" to listOf(
                            mapOf(
                                "value" to keycloakUser,
                                "temporary" to false,
                            ),
                        ),
                    ),
                )
                .post("/admin/realms/$realm/users")
                .thenReturn()
            createUserResponse.then().statusCode(HttpStatus.SC_CREATED)
            val userId = createUserResponse.getHeader("Location").split('/').last()

            role.agentRole?.let {
                grantRoleToUser(userId, it)
            }
        }
    }

    private fun grantRoleToUser(userId: String, role: AgentRole) {
        val clientRoleResponse = RestAssured.given().spec(requestBuilder)
            .param("search", role.roleName)
            .get("/admin/realms/$realm/clients/$clientId/roles")
            .thenReturn()
        clientRoleResponse.then().statusCode(HttpStatus.SC_OK)
        val clientRoleId = clientRoleResponse.body.jsonPath().getString("[0].id")

        RestAssured.given().spec(requestBuilder)
            .body(
                listOf(
                    mapOf(
                        "name" to role.roleName,
                        "id" to clientRoleId,
                    ),
                ),
            )
            .post("/admin/realms/$realm/users/$userId/role-mappings/clients/$clientId")
            .then().statusCode(HttpStatus.SC_NO_CONTENT)
    }
}

data class KeycloakPublicClientConfig(val redirectUris: List<String> = listOf())
