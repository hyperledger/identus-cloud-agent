package io.iohk.atala.shared

import java.util.Locale
import com.typesafe.config.ConfigMemorySize
import io.iohk.atala.shared.utils.BytesOps

import scala.collection.immutable.ArraySeq
import scala.util.matching.Regex

trait HashValue extends Any {

  def value: ArraySeq[Byte]

  override def toString: String = {
    BytesOps.bytesToHex(value)
  }

  override def equals(obj: Any): Boolean = {
    if (obj == null || obj.getClass != this.getClass)
      return false

    value.equals(obj.asInstanceOf[HashValue].value)
  }
}

trait HashValueFrom[A] {
  protected val config: HashValueConfig

  protected def constructor(value: ArraySeq[Byte]): A

  def from(string: String): Option[A] = {
    val lowercaseString = string.toLowerCase(Locale.ROOT)

    lowercaseString match {
      case config.HexPattern() =>
        val bytes = lowercaseString
          .grouped(2)
          .toList
          .map { hex =>
            Integer.parseInt(hex, 16).asInstanceOf[Byte]
          }
        Some(constructor(ArraySeq.from(bytes)))
      case _ => None
    }
  }

  def from(bytes: Iterable[Byte]): Option[A] = {
    if (bytes.size == config.size.toBytes) {
      Some(constructor(ArraySeq.from(bytes)))
    } else {
      None
    }
  }
}

case class HashValueConfig(size: ConfigMemorySize) {
  private[shared] val HexPattern: Regex = s"^[a-f0-9]{${2 * size.toBytes}}$$".r
}
