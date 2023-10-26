package interactions

import net.serenitybdd.annotations.Step
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.Tasks
import net.serenitybdd.screenplay.rest.abilities.CallAnApi

/**
 * This class is a copy of the class Post from serenity rest interactions
 * to add a custom authentication header to the request on-the-fly.
 */
open class Post(private val resource: String) : AuthRestInteraction() {
    @Step("{0} executes a POST on the resource #resource")
    override fun <T : Actor?> performAs(actor: T) {
        specWithAuthHeaders(actor).post(CallAnApi.`as`(actor).resolve(resource))
    }

    companion object {
        fun to(resource: String?): Post {
            return Tasks.instrumented(Post::class.java, resource)
        }
    }
}
