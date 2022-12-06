package extentions

import common.Environments
import io.cucumber.java.en.Given
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.rest.abilities.CallAnApi

object Agents {
    var agents: MutableMap<String,Actor> = mutableMapOf()
}
class WithAgents {
    @Given("{int} agents")
    fun agents(numberOfAgents: Int, agents: List<Map<String,String>>) {
        agents.forEach { agent ->
            val name = agent["name"].toString()
            val role = agent["role"].toString()
            if (!Agents.agents.containsKey(name)) {
                Agents.agents[name] =
                    Actor.named("${name}(${role})")
                        .whoCan(CallAnApi.at(Environments.urls[role]))
            }
        }
    }
}
