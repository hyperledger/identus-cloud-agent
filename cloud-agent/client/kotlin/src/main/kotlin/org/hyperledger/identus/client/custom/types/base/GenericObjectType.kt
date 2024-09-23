package org.hyperledger.identus.client.custom.types.base

import com.google.gson.Gson
import kotlin.reflect.KClass

interface GenericObjectType : BaseType {
    fun <T : Any> asObject(clazz: KClass<T>): T {
        val json = Gson().toJson(value)
        return Gson().fromJson(json, clazz.java)
    }
}
