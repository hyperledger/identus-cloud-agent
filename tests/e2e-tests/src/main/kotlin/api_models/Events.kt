package api_models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Event(
    var type: String,
    var id: String,
    var ts: String,
    var data: JsonElement,
    var walletId: String,
) : JsonEncoded

@Serializable
data class ConnectionEvent(
    var type: String,
    var id: String,
    var ts: String,
    var data: Connection,
    var walletId: String,
) : JsonEncoded

@Serializable
data class CredentialEvent(
    var type: String,
    var id: String,
    var ts: String,
    var data: Credential,
    var walletId: String,
) : JsonEncoded

@Serializable
data class PresentationEvent(
    var type: String,
    var id: String,
    var ts: String,
    var data: PresentationProof,
    var walletId: String,
) : JsonEncoded

@Serializable
data class DidEvent(
    var type: String,
    var id: String,
    var ts: String,
    var data: ManagedDid,
    var walletId: String,
) : JsonEncoded
