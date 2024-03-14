package models

import com.google.gson.annotations.SerializedName

class AnoncredsSchema(
    @SerializedName("name")
    val name: String,

    @SerializedName("version")
    val version: String,

    @SerializedName("issuerId")
    val issuerId: String,

    @SerializedName("attrNames")
    val attrNames: List<String>,
)
