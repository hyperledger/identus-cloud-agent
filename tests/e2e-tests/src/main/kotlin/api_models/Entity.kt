package api_models

import kotlinx.serialization.Serializable

@Serializable
data class CreateEntityRequest(
    val walletId: String,
    val name: String,
    val id: String,
): JsonEncoded

@Serializable
data class AddApiKeyRequest(
    val entityId: String,
    val apiKey: String,
): JsonEncoded
