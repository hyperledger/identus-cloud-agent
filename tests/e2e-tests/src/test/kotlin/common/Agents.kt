package common

import io.restassured.builder.RequestSpecBuilder
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.abilities.CallAnApi

object Agents {
    lateinit var Acme: Actor
        private set
    lateinit var Bob: Actor
        private set
    lateinit var Mallory: Actor
        private set

    init {
        if (Environments.AGENT_AUTH_REQUIRED) {
            SerenityRest.setDefaultRequestSpecification(
                RequestSpecBuilder().addHeader(
                    Environments.AGENT_AUTH_HEADER,
                    Environments.AGENT_AUTH_KEY)
                    .build()
            )
        }
    }

    fun createAgents() {
        Acme = Actor.named("Acme").whoCan(CallAnApi.at(Environments.ACME_AGENT_URL))
        Bob = Actor.named("Bob").whoCan(CallAnApi.at(Environments.BOB_AGENT_URL))
        Mallory = Actor.named("Mallory").whoCan(CallAnApi.at(Environments.MALLORY_AGENT_URL))
    }
}
