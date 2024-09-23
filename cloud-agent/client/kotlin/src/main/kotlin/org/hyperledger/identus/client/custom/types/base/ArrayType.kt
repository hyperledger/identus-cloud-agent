package org.hyperledger.identus.client.custom.types.base

import org.hyperledger.identus.client.custom.convert

interface ArrayType : BaseType {
    fun asArray(): kotlin.collections.List<Any>? {
        return convert()
    }
}
