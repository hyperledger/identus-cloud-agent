package interactions

import io.restassured.specification.RequestSpecification
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.interactions.RestInteraction

abstract class AuthRestInteraction : RestInteraction() {

    fun <T : Actor?> specWithAuthHeaders(actor: T): RequestSpecification {
        val spec = rest()
        if (actor!!.recall<String>("BEARER_TOKEN") != null) {
            spec.header("Authorization", "Bearer ${actor.recall<String>("BEARER_TOKEN")}")
        }
        if (actor.recall<String>("AUTH_KEY") != null) {
            spec.header(actor.recall("AUTH_HEADER"), actor.recall("AUTH_KEY"))
        }
        return spec
    }
}
