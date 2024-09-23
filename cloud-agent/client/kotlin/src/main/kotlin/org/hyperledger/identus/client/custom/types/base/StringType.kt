package org.hyperledger.identus.client.custom.types.base

import org.hyperledger.identus.client.custom.convert

interface StringType : BaseType {
    fun asString(): kotlin.String? {
        return convert()
    }
}
