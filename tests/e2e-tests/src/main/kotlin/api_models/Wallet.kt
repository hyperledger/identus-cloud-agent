package api_models

import kotlinx.serialization.Serializable

@Serializable
data class CreateWalletRequest(
    val name: String,
    val seed: String,
    val id: String,
): JsonEncoded
