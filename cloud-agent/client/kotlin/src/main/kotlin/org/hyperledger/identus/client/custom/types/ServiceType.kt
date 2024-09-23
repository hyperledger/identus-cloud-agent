package org.hyperledger.identus.client.custom.types

import com.google.gson.annotations.JsonAdapter
import org.hyperledger.identus.client.custom.adapters.ServiceTypeAdapter
import org.hyperledger.identus.client.custom.types.base.ArrayType
import org.hyperledger.identus.client.custom.types.base.StringType

@JsonAdapter(ServiceTypeAdapter::class)
class ServiceType(override var value: Any?) : ArrayType, StringType
