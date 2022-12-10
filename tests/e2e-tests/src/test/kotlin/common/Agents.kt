package common

import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.abilities.CallAnApi

object Agents {
    lateinit var Acme: Actor
        private set
    lateinit var Bob: Actor
        private set
    lateinit var Mallory: Actor
        private set

    fun createAgents() {
        Acme = Actor.named("Acme").whoCan(CallAnApi.at(Environments.ACME_AGENT_URL))
        Bob = Actor.named("Bob").whoCan(CallAnApi.at(Environments.BOB_AGENT_URL))
        Mallory = Actor.named("Mallory").whoCan(CallAnApi.at(Environments.MALLORY_AGENT_URL))
    }
}
