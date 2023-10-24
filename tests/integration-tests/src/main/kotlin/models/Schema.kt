package models

import com.google.gson.annotations.SerializedName

data class Schema(
    @SerializedName("\$id")
    var id: String = "",

    @SerializedName("\$schema")
    var schema: String = "",

    @SerializedName("\$description")
    var description: String = "",

    @SerializedName("type")
    var type: String = "",

    @SerializedName("properties")
    val properties: MutableMap<String, SchemaProperty> = mutableMapOf()
)
