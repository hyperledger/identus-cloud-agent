package org.hyperledger.identus.client.custom.types.base

interface BoolType : BaseType {
    fun asBool(): Boolean {
        return value as Boolean
    }
}
