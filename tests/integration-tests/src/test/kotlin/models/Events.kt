package models

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.hyperledger.identus.client.models.Connection
import org.hyperledger.identus.client.models.IssueCredentialRecord
import org.hyperledger.identus.client.models.ManagedDID
import org.hyperledger.identus.client.models.PresentationStatus

data class Event(
    @SerializedName("type") var type: String,
    @SerializedName("id") var id: String,
    @SerializedName("ts") var ts: String,
    @SerializedName("data") var data: JsonElement,
    @SerializedName("walletId") var walletId: String,
)

data class ConnectionEvent(
    @SerializedName("type") var type: String,
    @SerializedName("id") var id: String,
    @SerializedName("ts") var ts: String,
    @SerializedName("data") var data: Connection,
    @SerializedName("walletId") var walletId: String,
)

data class CredentialEvent(
    @SerializedName("type") var type: String,
    @SerializedName("id") var id: String,
    @SerializedName("ts") var ts: String,
    @SerializedName("data") var data: IssueCredentialRecord,
    @SerializedName("walletId") var walletId: String,
)

data class PresentationEvent(
    @SerializedName("type") var type: String,
    @SerializedName("id") var id: String,
    @SerializedName("ts") var ts: String,
    @SerializedName("data") var data: PresentationStatus,
    @SerializedName("walletId") var walletId: String,
)

data class DidEvent(
    @SerializedName("type") var type: String,
    @SerializedName("id") var id: String,
    @SerializedName("ts") var ts: String,
    @SerializedName("data") var data: ManagedDID,
    @SerializedName("walletId") var walletId: String,
)
