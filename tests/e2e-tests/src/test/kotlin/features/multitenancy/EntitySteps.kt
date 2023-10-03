package features.multitenancy

import api_models.CreateEntityRequest
import api_models.AddApiKeyRequest
import common.Ensure
import common.Utils
import interactions.Post
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor
import org.apache.http.HttpStatus.SC_CREATED
import java.util.*

class EntitySteps {

    fun createNewEntity(
        actor: Actor,
        walletId: String,
        name: String = "",
        id: String = UUID.randomUUID().toString()): String {
        actor.attemptsTo(
            Post.to("/iam/entities")
                .with {
                    it.body(
                        CreateEntityRequest(
                            walletId = walletId,
                            name = name,
                            id = id,
                        )
                    )
                },
        )
        actor.attemptsTo(
            Ensure.that(SerenityRest.lastResponse().statusCode).isEqualTo(SC_CREATED)
        )
        return Utils.lastResponseObject("id", String::class)
    }

    fun addNewApiKeyToEntity(actor: Actor, entityId: String, apiKey: String) {
        actor.attemptsTo(
            Post.to("/iam/apikey-authentication")
                .with {
                    it.body(
                        AddApiKeyRequest(
                            entityId = entityId,
                            apiKey = apiKey,
                        )
                    )
                },
        )
        actor.attemptsTo(
            Ensure.that(SerenityRest.lastResponse().statusCode).isEqualTo(SC_CREATED)
        )
    }
}
