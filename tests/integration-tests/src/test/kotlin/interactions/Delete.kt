package interactions

import net.serenitybdd.annotations.Step
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.Tasks
import net.serenitybdd.screenplay.rest.abilities.CallAnApi

/**
 * This class is a copy of the class Delete from serenity rest interactions
 * to add a custom authentication header to the request on-the-fly.
 */
open class Delete(private val resource: String) : AuthRestInteraction() {
    @Step("{0} executes a DELETE on the resource #resource")
    override fun <T : Actor?> performAs(actor: T) {
        specWithAuthHeaders(actor).delete(CallAnApi.`as`(actor).resolve(resource))
    }

    companion object {
        fun from(resource: String?): Delete {
            return Tasks.instrumented(Delete::class.java, resource)
        }
    }
}
