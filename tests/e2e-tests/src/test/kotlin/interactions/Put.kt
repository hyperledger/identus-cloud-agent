package interactions

import common.Environments
import io.ktor.util.*
import net.serenitybdd.annotations.Step
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.Tasks
import net.serenitybdd.screenplay.rest.abilities.CallAnApi
import net.serenitybdd.screenplay.rest.interactions.RestInteraction


/**
 * This class is a copy of the class Put from serenity rest interactions
 * to add a custom authentication header to the request on-the-fly.
 */
open class Put(private val resource: String) : RestInteraction() {
    @Step("{0} executes a PUT on the resource #resource")
    override fun <T : Actor?> performAs(actor: T) {
        val spec = rest()
        if (actor!!.name.toLowerCasePreservingASCIIRules().contains("admin")) {
            spec.header(Environments.ADMIN_AUTH_HEADER, Environments.ADMIN_AUTH_TOKEN)
        } else {
            if (Environments.AGENT_AUTH_REQUIRED) {
                spec.header(Environments.AGENT_AUTH_HEADER, actor.recall("AUTH_KEY"))
            }
        }
        spec.put(CallAnApi.`as`(actor).resolve(resource))
    }

    companion object {
        fun to(resource: String?): Put {
            return Tasks.instrumented(Put::class.java, resource)
        }
    }
}
