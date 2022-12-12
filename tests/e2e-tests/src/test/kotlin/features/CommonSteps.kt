package features

import common.Agents.Acme
import common.Agents.Bob
import common.Agents.Mallory
import common.Agents.createAgents
import io.cucumber.java.Before
import io.cucumber.java.ParameterType
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.actors.Cast
import net.serenitybdd.screenplay.actors.OnStage

class CommonSteps {
    @Before
    fun setStage() {
        createAgents()
        val cast = object: Cast() {
            override fun getActors(): MutableList<Actor> {
                return mutableListOf(Acme, Bob, Mallory)
            }
        }
        OnStage.setTheStage(cast)
    }

    @ParameterType(".*")
    fun actor(actorName: String): Actor {
        return OnStage.theActorCalled(actorName);
    }
}
