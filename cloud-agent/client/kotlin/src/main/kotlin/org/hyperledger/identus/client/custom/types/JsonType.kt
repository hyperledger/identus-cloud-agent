package org.hyperledger.identus.client.custom.types

import com.google.gson.annotations.JsonAdapter
import org.hyperledger.identus.client.custom.adapters.JsonTypeAdapter
import org.hyperledger.identus.client.custom.types.base.ArrayType
import org.hyperledger.identus.client.custom.types.base.BoolType
import org.hyperledger.identus.client.custom.types.base.GenericObjectType
import org.hyperledger.identus.client.custom.types.base.NullType
import org.hyperledger.identus.client.custom.types.base.NumberType
import org.hyperledger.identus.client.custom.types.base.StringType

@JsonAdapter(JsonTypeAdapter::class)
class JsonType(override var value: Any?) : ArrayType, BoolType, NullType, NumberType, GenericObjectType, StringType
