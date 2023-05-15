package io.iohk.atala.castor.core.model.did

sealed trait ServiceType

object ServiceType {

  opaque type Name = String

  object Name {
    def fromStringUnsafe(name: String): Name = name

    def fromString(name: String): Either[String, Name] = {
      if (name.trim().isEmpty()) Left("service type name must have at least a non whitespace character")
      else if (name.take(1).isBlank() || name.takeRight(1).isBlank())
        Left("service type name cannot start nor end with whitespaces")
      else Right(name)
    }
  }

  extension (name: Name) {
    def value: String = name
  }

  final case class Single(value: Name) extends ServiceType
  final case class Multiple(head: Name, tail: Seq[Name]) extends ServiceType {
    def values: Seq[Name] = head +: tail
  }
}
