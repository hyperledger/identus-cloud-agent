package interactions

import com.sksamuel.hoplite.ConfigLoader
import config.Config
import io.ktor.util.*
import io.restassured.specification.RequestSpecification
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.interactions.RestInteraction

abstract class AuthRestInteraction : RestInteraction() {

    private val config = ConfigLoader().loadConfigOrThrow<Config>("/tests.conf")

    fun <T : Actor?> specWithAuthHeaders(actor: T): RequestSpecification {
        val spec = rest()
        if (actor!!.name.toLowerCasePreservingASCIIRules().contains("admin")) {
            spec.header(config.global.adminAuthHeader, config.admin.apikey)
        } else {
            if (config.global.authRequired) {
                spec.header(config.global.authHeader, actor.recall("AUTH_KEY"))
            }
        }
        return spec
    }
}
