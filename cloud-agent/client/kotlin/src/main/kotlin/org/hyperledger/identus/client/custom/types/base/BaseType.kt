package org.hyperledger.identus.client.custom.types.base

import com.google.gson.annotations.JsonAdapter
import org.hyperledger.identus.client.custom.adapters.JsonTypeAdapter

@JsonAdapter(JsonTypeAdapter::class)
interface BaseType {
    var value: Any?

}
