/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport",
)

package org.hyperledger.identus.client.models

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * A patch to existing Service. 'type' and 'serviceEndpoint' cannot both be empty.
 *
 * @param id The id of the service to update
 * @param type
 * @param serviceEndpoint
 */

data class UpdateManagedDIDServiceAction(

    /* The id of the service to update */
    @SerializedName("id")
    val id: kotlin.String,

    @SerializedName("type")
    val type: kotlin.collections.List<kotlin.String>? = null,

    @SerializedName("serviceEndpoint")
    val serviceEndpoint: JsonElement? = null,

)
