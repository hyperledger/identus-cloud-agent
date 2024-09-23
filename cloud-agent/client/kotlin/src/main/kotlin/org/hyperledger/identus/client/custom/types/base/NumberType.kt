package org.hyperledger.identus.client.custom.types.base

interface NumberType : BaseType {
    fun asNumber(): kotlin.Number {
        return value as kotlin.Number
    }
}
