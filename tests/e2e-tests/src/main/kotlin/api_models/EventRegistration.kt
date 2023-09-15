package api_models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterWebhookRequest(
    val url: String,
) :JsonEncoded
