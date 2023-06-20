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
    lateinit var Faber: Actor
        private set

    fun createAgents() {
        Acme = Actor.named("Acme").whoCan(CallAnApi.at(Environments.ACME_AGENT_URL))
        Bob = Actor.named("Bob").whoCan(CallAnApi.at(Environments.BOB_AGENT_URL))
        Mallory = Actor.named("Mallory").whoCan(CallAnApi.at(Environments.MALLORY_AGENT_URL))
        Faber = Actor.named("Faber").whoCan(CallAnApi.at(Environments.FABER_AGENT_URL))
        if (Environments.AGENT_AUTH_REQUIRED) {
            Acme.remember("AUTH_KEY", Environments.ACME_AUTH_KEY)
            Bob.remember("AUTH_KEY", Environments.BOB_AUTH_KEY)
            Mallory.remember("AUTH_KEY", Environments.MALLORY_AUTH_KEY)
            Faber.remember("AUTH_KEY", Environments.FABER_AUTH_KEY)
        }
    }
}
