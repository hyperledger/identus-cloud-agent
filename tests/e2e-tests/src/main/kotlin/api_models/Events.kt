package api_models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Event (
    var type: String,
    var id: String,
    var ts: String,
    var data: JsonElement,
): JsonEncoded

@Serializable
data class ConnectionEvent (
    var type: String,
    var id: String,
    var ts: String,
    var data: Connection,
): JsonEncoded

@Serializable
data class CredentialEvent (
    var type: String,
    var id: String,
    var ts: String,
    var data: Credential,
): JsonEncoded

@Serializable
data class PresentationEvent (
    var type: String,
    var id: String,
    var ts: String,
    var data: PresentationProof,
): JsonEncoded

@Serializable
data class DidEvent (
    var type: String,
    var id: String,
    var ts: String,
    var data: ManagedDid,
): JsonEncoded
