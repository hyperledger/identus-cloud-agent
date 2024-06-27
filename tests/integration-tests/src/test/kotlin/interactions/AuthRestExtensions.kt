package interactions

import net.serenitybdd.screenplay.rest.interactions.RestInteraction

fun Post.body(obj: Any): RestInteraction {
    return this.with {
        it.header("Content-Type", "application/json").body(obj)
    }
}

fun Patch.body(obj: Any): RestInteraction {
    return this.with {
        it.header("Content-Type", "application/json").body(obj)
    }
}
