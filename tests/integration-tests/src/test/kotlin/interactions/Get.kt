package interactions

import net.serenitybdd.annotations.Step
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.Tasks
import net.serenitybdd.screenplay.rest.abilities.CallAnApi

/**
 * This class is a copy of the class Get from serenity rest interactions
 * to add a custom authentication header to the request on-the-fly.
 */
open class Get(private val resource: String) : AuthRestInteraction() {
    @Step("{0} executes a GET on the resource #resource")
    override fun <T : Actor?> performAs(actor: T) {
        specWithAuthHeaders(actor).get(CallAnApi.`as`(actor).resolve(resource))
    }

    companion object {
        fun resource(resource: String?): Get {
            return Tasks.instrumented(Get::class.java, resource)
        }
    }
}
