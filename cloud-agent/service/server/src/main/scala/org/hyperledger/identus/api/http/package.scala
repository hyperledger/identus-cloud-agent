package org.hyperledger.identus.api

// import sttp.tapir.generic.auto._
import sttp.tapir.Schema
import sttp.tapir.Schema.annotations.{customise, description, encodedExample}
import sttp.tapir.Validator

import scala.annotation.StaticAnnotation
import scala.deriving.Mirror

//encodedExample(JsonEncoder
//0527aea1-d131-3948-a34d-03af39aba8b5

//diff -y cloud-agent/service/api/http/cloud-agent-openapi-spec3.yaml cloud-agent/service/api/http/cloud-agent-openapi-spec2.yaml --suppress-common-lines
package object http {

  trait Annotation[E] {
    def description: String
    def example: Option[String]
    def validator: Validator[E]
    def schema: Schema[E]

    // class api extends customise(_ => s)
  }

  class AnnotationWithExample[E](
      description: String,
      example: String,
      validator: Validator[E] = Validator.pass[E]
  )(using s: Schema[E])
      extends Annotation[E] {
    def schema: Schema[E] = s
      .encodedExample(example)
      .description(description)
      .validate(validator)

  }

  class AnnotationWithoutExample[E](
      description: String,
      validator: Validator[E] = Validator.pass[E]
  )(using s: Schema[E])
      extends Annotation[E] {
    def schema: Schema[E] = s
      .description(description)
      .validate(validator)
  }

  object Annotation {
    def apply[E](
        description: String,
        example: String,
        validator: Validator[E] = Validator.pass[E]
    )(using s: Schema[E]) =
      AnnotationWithExample(description, example, validator)

    def apply[E](description: String)(using s: Schema[E]) =
      AnnotationWithoutExample(description, Validator.pass[E])
  }

  val DIDRefRegex = """^did:(?<method>[a-z0-9]+(:[a-z0-9]+)*)\:(?<idstring>[^#?]*)$"""
  val DIDRegex = """^did:(?<method>[a-z0-9]+(:[a-z0-9]+)*)\:(?<idstring>[^#?]*)?(?<query>\?[^#]*)?(?<fragment>\#.*)?$"""
  val SemVerRegex =
    """^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$"""
}
