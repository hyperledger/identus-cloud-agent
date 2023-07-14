package api_models

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed interface JsonEncoded {
    fun toJsonString(): String {
        return Json.encodeToString(this)
    }
}
