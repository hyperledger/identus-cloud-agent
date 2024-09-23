package org.hyperledger.identus.client.custom.adapters

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import org.hyperledger.identus.client.custom.readExtension
import org.hyperledger.identus.client.custom.types.ServiceType
import org.hyperledger.identus.client.custom.writeExtension

class ServiceTypeAdapter : TypeAdapter<ServiceType>() {
    override fun write(out: JsonWriter, value: ServiceType) = writeExtension(out, value)
    override fun read(input: JsonReader): ServiceType = readExtension(input)
}
