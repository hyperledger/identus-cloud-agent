package api_models

data class Invitation(
    var id: String = "",
    var from: String = "",
    var invitationUrl: String = "",
    var type: String = "",
)
