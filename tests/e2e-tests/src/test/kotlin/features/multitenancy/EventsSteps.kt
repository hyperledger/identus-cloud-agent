package features.multitenancy

import api_models.RegisterWebhookRequest
import interactions.Post
import net.serenitybdd.rest.SerenityRest
import net.serenitybdd.screenplay.Actor

class EventsSteps {
    fun registerNewWebhook(actor: Actor, webhookUrl: String) {
        actor.attemptsTo(
            Post.to("/events/webhooks")
                .with {
                    it.body(
                        RegisterWebhookRequest(url = webhookUrl)
                    )
                },
        )
    }
}
