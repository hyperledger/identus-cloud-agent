package org.hyperledger.identus.client.custom.adapters

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.hyperledger.identus.client.custom.readExtension
import org.hyperledger.identus.client.custom.types.JsonType
import org.hyperledger.identus.client.custom.writeExtension

class JsonTypeAdapter : TypeAdapter<JsonType>() {
    override fun write(out: JsonWriter, value: JsonType) = writeExtension(out, value)

    override fun read(input: JsonReader): JsonType = readExtension(input)
}
