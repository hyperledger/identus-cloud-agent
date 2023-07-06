package api_models
data class Connection(
    var connectionId: String = "",
    var thid: String = "",
    var createdAt: String = "",
    var updatedAt: String = "",
    var invitation: Invitation = Invitation(),
    var kind: String = "",
    var self: String = "",
    var state: String = "",
    var label: String = "",
    var myDid: String = "",
    var theirDid: String = "",
    var role: String = "",
)

object ConnectionState {
    const val INVITATION_GENERATED = "InvitationGenerated"
    const val CONNECTION_REQUEST_PENDING = "ConnectionRequestPending"
    const val CONNECTION_RESPONSE_SENT = "ConnectionResponseSent"
    const val CONNECTION_RESPONSE_RECEIVED = "ConnectionResponseReceived"
}
