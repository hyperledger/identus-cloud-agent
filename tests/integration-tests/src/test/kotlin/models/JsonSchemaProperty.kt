package models

import com.google.gson.annotations.SerializedName

data class JsonSchemaProperty(
    @SerializedName("type")
    var type: String = "",
)
