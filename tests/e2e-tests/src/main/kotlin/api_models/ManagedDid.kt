package api_models

data class ManagedDid(
    var did: String = "",
    var longFormDid: String = "",
    var status: String = "",
)

object ManagedDidStatuses {
    val PUBLISHED = "PUBLISHED"
    val UNPUBLISHED = "UNPUBLISHED"
}