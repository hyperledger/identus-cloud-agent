package api_models

data class Connection(
    var connectionId: String = "",
    var createdAt: String = "",
    var invitation: Invitation = Invitation(),
    var kind: String = "",
    var self: String = "",
    var state: String = "",
    var label: String = "",
    var myDid: String = "",
    var theirDid: String = ""
)
