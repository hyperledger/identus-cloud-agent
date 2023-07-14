package api_models

import kotlinx.serialization.Serializable

@Serializable
data class Invitation(
    var id: String = "",
    var from: String = "",
    var invitationUrl: String = "",
    var type: String = "",
): JsonEncoded
