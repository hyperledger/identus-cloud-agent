package interactions

import net.serenitybdd.annotations.Step
import net.serenitybdd.screenplay.*

open class Notes : Performable {
    private lateinit var description: String
    constructor()

    constructor(description: String) {
        this.description = description
    }
    @Step("{0} takes notes of #description")
    override fun <T : Actor> performAs(actor: T) {}
}

object Take {
    fun notes(description: String): Performable {
        return Notes(description)
    }
}
