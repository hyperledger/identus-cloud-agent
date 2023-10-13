package models

import com.google.gson.annotations.SerializedName

data class SchemaProperty(
    @SerializedName("type")
    var type: String = ""
)
