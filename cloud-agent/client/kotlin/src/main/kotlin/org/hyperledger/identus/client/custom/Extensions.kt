package org.hyperledger.identus.client.custom

import com.google.gson.Gson
import com.google.gson.ToNumberPolicy
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonToken.BEGIN_ARRAY
import com.google.gson.stream.JsonToken.BEGIN_OBJECT
import com.google.gson.stream.JsonToken.BOOLEAN
import com.google.gson.stream.JsonToken.NULL
import com.google.gson.stream.JsonToken.NUMBER
import com.google.gson.stream.JsonToken.STRING
import com.google.gson.stream.JsonWriter
import org.hyperledger.identus.client.custom.adapters.JsonTypeAdapter
import org.hyperledger.identus.client.custom.types.JsonType
import org.hyperledger.identus.client.custom.types.base.BaseType

inline fun <reified T : Any> BaseType.convert(): T? {
    if (this.value == null) {
        return null
    }
    if (this.value is T) {
        return this.value as T
    }
    throw IllegalStateException("Requested parameter as [${this.value!!::class.simpleName}] but is [${T::class.simpleName}]")
}

inline fun <reified T: BaseType> TypeAdapter<T>.writeExtension(out: JsonWriter, value: T) {
    out.jsonValue(Gson().toJson(value.value))
}

fun JsonTypeAdapter.oleole() {
    println("PORRA")
}

inline fun <reified T: BaseType> TypeAdapter<T>.readExtension(input: JsonReader): T {
    val constructor = T::class.constructors.first()
    when (val parsed = input.parse(input)) {
        is List<*> -> {
            val json = Gson().toJson(parsed)
            val type = object : TypeToken<List<Any?>>() {}.type
            val list: List<Any?> = Gson().fromJson(json, type)
            return constructor.call(list)
        }
        null,
        is String,
        is Number,
        is Boolean,
        is HashMap<*,*> -> return constructor.call(parsed)
        else -> throw IllegalArgumentException("Unsupported type ${parsed?.javaClass}")
    }
}

fun JsonReader.parse(input: JsonReader): Any? {
    val token: JsonToken = input.peek()
    when (token) {
        BEGIN_ARRAY -> {
            val list = mutableListOf<Any?>()
            input.beginArray()

            while (input.hasNext()) {
                list.add(parse(input))
            }

            input.endArray()
            return list
        }
        BEGIN_OBJECT -> {
            val map = linkedMapOf<Any, Any?>()
            input.beginObject()
            while (input.hasNext()) {
                map[input.nextName()] = parse(input)
            }
            input.endObject()
            return map
        }
        STRING -> return input.nextString()
        NUMBER -> return ToNumberPolicy.LAZILY_PARSED_NUMBER.readNumber(input)
        BOOLEAN -> return input.nextBoolean()
        NULL -> {
            input.nextNull()
            return null
        }
        else -> throw IllegalStateException()
    }
}

