package io.iohk.atala.shared.models

import io.iohk.atala.shared.utils.BytesOps
import scala.util.Try

opaque type HexString = String

object HexString {
  def fromStringUnsafe(s: String): HexString = s
  def fromString(s: String): Try[HexString] = Try(BytesOps.hexToBytes(s)).map(_ => s)
  def fromByteArray(bytes: Array[Byte]): HexString = BytesOps.bytesToHex(bytes)

  extension (s: HexString) {
    def toByteArray: Array[Byte] = BytesOps.hexToBytes(s)
    def toString: String = s
  }
}
