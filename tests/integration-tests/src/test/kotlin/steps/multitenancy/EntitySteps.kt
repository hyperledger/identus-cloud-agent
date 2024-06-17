package steps.multitenancy

import interactions.Post
import io.iohk.atala.automation.extensions.get
import io.iohk.atala.automation.serenity.ensure.Ensure
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.SC_CREATED
import org.hyperledger.identus.client.models.ApiKeyAuthenticationRequest
import org.hyperledger.identus.client.models.CreateEntityRequest
import org.hyperledger.identus.client.models.EntityResponse
import java.util.*

class EntitySteps {

    fun createNewEntity(
        actor: Actor,
        walletId: UUID,
        name: String = "",
        id: UUID = UUID.randomUUID(),
    ): EntityResponse {
        actor.attemptsTo(
            Post.to("/iam/entities")
                .with {
                    it.body(
                        CreateEntityRequest(
                            walletId = walletId,
                            name = name,
                            id = id,
                        ),
                    )
                },
        )
        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )
        return SerenityRest.lastResponse().get<EntityResponse>()
    }

    fun addNewApiKeyToEntity(actor: Actor, entityId: UUID, apiKey: String) {
        actor.attemptsTo(
            Post.to("/iam/apikey-authentication")
                .with {
                    it.body(
                        ApiKeyAuthenticationRequest(
                            entityId = entityId,
                            apiKey = apiKey,
                        ),
                    )
                },
        )
        actor.attemptsTo(
            Ensure.thatTheLastResponse().statusCode().isEqualTo(SC_CREATED),
        )
    }
}
