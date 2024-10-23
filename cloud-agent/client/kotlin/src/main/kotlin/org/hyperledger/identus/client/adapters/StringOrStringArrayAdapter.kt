package org.hyperledger.identus.client.adapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializer
import com.google.gson.JsonNull
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import java.lang.reflect.Type

class StringOrStringArrayAdapter : JsonSerializer<List<String>>, JsonDeserializer<List<String>> {

    // Deserialize logic: String or Array of Strings to List<String>
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): List<String> {
        return when {
            json.isJsonArray -> context.deserialize(json, typeOfT)
            json.isJsonPrimitive -> listOf(json.asString)
            json.isJsonNull -> emptyList()
            else -> throw JsonParseException("Unexpected type for field")
        }
    }

    // Serialize logic: List<String> to String or Array of Strings
    override fun serialize(src: List<String>?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return when {
            src.isNullOrEmpty() -> JsonNull.INSTANCE
            src.size == 1 -> JsonPrimitive(src[0]) // If only one string, serialize as a single string
            else -> context!!.serialize(src) // Otherwise, serialize as a list
        }
    }
}