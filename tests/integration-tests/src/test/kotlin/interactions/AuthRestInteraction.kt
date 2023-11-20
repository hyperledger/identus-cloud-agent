package interactions

import com.sksamuel.hoplite.ConfigLoader
import config.Config
import io.restassured.specification.RequestSpecification
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.interactions.RestInteraction

abstract class AuthRestInteraction : RestInteraction() {

    private val config = ConfigLoader().loadConfigOrThrow<Config>(System.getenv("INTEGRATION_TESTS_CONFIG") ?: "/configs/basic.conf")

    fun <T : Actor?> specWithAuthHeaders(actor: T): RequestSpecification {
        val spec = rest()
        if (config.services.keycloak != null && actor!!.recall<String>("KEYCLOAK_BEARER_TOKEN") != null) {
            spec.header("Authorization", "Bearer ${actor!!.recall<String>("KEYCLOAK_BEARER_TOKEN")}")
        }
        if (actor!!.recall<String>("AUTH_KEY") != null) {
            spec.header(actor.recall("AUTH_HEADER"), actor.recall("AUTH_KEY"))
        }
        return spec
    }
}
