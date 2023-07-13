package api_models

import kotlinx.serialization.Serializable

@Serializable
data class ManagedDid(
    var did: String = "",
    var longFormDid: String = "",
    var status: String = "",
): JsonEncoded

object ManagedDidStatuses {
    val PUBLISHED = "PUBLISHED"
    val CREATED = "CREATED"
}
