package org.hyperledger.identus.shared.models

opaque type KeyId = String
object KeyId:
  def apply(value: String): KeyId = value
  extension (id: KeyId) def value: String = id
