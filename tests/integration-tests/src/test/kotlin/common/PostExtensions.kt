package common

import interactions.Post
import net.serenitybdd.screenplay.rest.interactions.RestInteraction

fun Post.body(obj: Any): RestInteraction {
    return this.with { request ->
        request.header("Content-Type", "application/json").body(obj)
    }
}
