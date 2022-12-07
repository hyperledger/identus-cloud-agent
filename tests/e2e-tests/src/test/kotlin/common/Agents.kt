package common

import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.abilities.CallAnApi

object Agents {
    val Acme = Actor.named("Acme").whoCan(CallAnApi.at(Environments.ACME_AGENT_URL))
    val Bob = Actor.named("Bob").whoCan(CallAnApi.at(Environments.BOB_AGENT_URL))
    val Mallory = Actor.named("Mallory").whoCan(CallAnApi.at(Environments.MALLORY_AGENT_URL))
}
