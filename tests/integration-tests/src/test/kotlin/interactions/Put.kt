package interactions

import net.serenitybdd.annotations.Step
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.Tasks
import net.serenitybdd.screenplay.rest.abilities.CallAnApi

/**
 * This class is a copy of the class Put from serenity rest interactions
 * to add a custom authentication header to the request on-the-fly.
 */
open class Put(private val resource: String) : AuthRestInteraction() {
    @Step("{0} executes a PUT on the resource #resource")
    override fun <T : Actor?> performAs(actor: T) {
        specWithAuthHeaders(actor).put(CallAnApi.`as`(actor).resolve(resource))
    }

    companion object {
        fun to(resource: String?): Put {
            return Tasks.instrumented(Put::class.java, resource)
        }
    }
}
