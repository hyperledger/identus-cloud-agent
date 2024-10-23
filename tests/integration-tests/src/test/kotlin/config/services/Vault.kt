package config.services

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus
import com.sksamuel.hoplite.ConfigAlias
import config.VaultAuthType
import io.iohk.atala.automation.utils.Logger
import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.specification.RequestSpecification
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File

data class Vault(
    @ConfigAlias("http_port") val httpPort: Int,
    @ConfigAlias("vault_auth_type") val authType: VaultAuthType = VaultAuthType.APP_ROLE,
) : ServiceBase() {
    private val logger = Logger.get<Vault>()
    override val logServices: List<String> = listOf("vault")
    private val vaultComposeFile: String = "src/test/resources/containers/vault.yml"
    override val container: ComposeContainer = ComposeContainer(File(vaultComposeFile)).withEnv(
        mapOf(
            "VAULT_PORT" to httpPort.toString(),

        ),
    ).waitingFor(
        "vault",
        Wait.forHealthcheck(),
    )
    private val vaultBaseUrl = "http://localhost:$httpPort/"
    private var requestBuilder: RequestSpecification? = null
    private val appRoleName = "agent"
    private val appRolePolicyName = "agent-policy"

    override fun postStart() {
        if (authType == VaultAuthType.APP_ROLE) {
            logger.info("Setting up Vault app roles")
            initRequestBuilder()
            enableAppRoleAuth()
            createAppRolePolicy()
            createAppRole()
        }
    }

    private fun initRequestBuilder() {
        requestBuilder = RequestSpecBuilder()
            .setBaseUri(vaultBaseUrl)
            .setContentType("application/json")
            .addHeader("x-vault-token", "root")
            .build()
    }

    private fun enableAppRoleAuth() {
        RestAssured.given().spec(requestBuilder)
            .body(
                mapOf(
                    "type" to "approle",
                ),
            )
            .post("/v1/sys/auth/approle")
            .then().statusCode(HttpStatus.SC_NO_CONTENT)
    }

    private fun createAppRolePolicy() {
        RestAssured.given().spec(requestBuilder)
            .body(
                mapOf(
                    "policy" to """
                        path "secret/*" {
                            capabilities = ["create", "read", "update", "patch", "delete", "list"]
                        }
                        """,
                ),
            )
            .post("/v1/sys/policy/$appRolePolicyName")
            .then().statusCode(HttpStatus.SC_NO_CONTENT)
    }

    private fun createAppRole() {
        RestAssured.given().spec(requestBuilder)
            .body(
                mapOf(
                    "token_policies" to appRolePolicyName,
                    "token_ttl" to "60s",
                ),
            )
            .post("/v1/auth/approle/role/$appRoleName")
            .then().statusCode(HttpStatus.SC_NO_CONTENT)

        // fixed roleId of appRole
        RestAssured.given().spec(requestBuilder)
            .body(
                mapOf(
                    "role_id" to "agent",
                ),
            )
            .post("/v1/auth/approle/role/$appRoleName/role-id")
            .then().statusCode(HttpStatus.SC_NO_CONTENT)

        // fixed secretId of appRole
        RestAssured.given().spec(requestBuilder)
            .body(
                mapOf(
                    "secret_id" to "agent-secret",
                ),
            )
            .post("/v1/auth/approle/role/$appRoleName/custom-secret-id")
            .then().statusCode(HttpStatus.SC_OK)
    }
}
