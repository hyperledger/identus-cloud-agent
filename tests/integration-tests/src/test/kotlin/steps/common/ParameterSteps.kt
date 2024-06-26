package steps.common

import io.cucumber.java.DataTableType
import io.cucumber.java.ParameterType
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.actors.OnStage
import org.hyperledger.identus.client.models.*

class ParameterSteps {
    @ParameterType(".*")
    fun actor(actorName: String): Actor {
        return OnStage.theActorCalled(actorName)
    }

    @ParameterType(".*")
    fun curve(value: String): Curve {
        return Curve.decode(value) ?: throw IllegalArgumentException("$value is not a valid Curve value")
    }

    @ParameterType(".*")
    fun purpose(value: String): Purpose {
        return Purpose.decode(value) ?: throw IllegalArgumentException("$value is not a valid Purpose value")
    }

    @DataTableType
    fun vcVerification(cell: String): VcVerification {
        return VcVerification.valueOf(cell)
    }
}
