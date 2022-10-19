package io.iohk.atala.pollux.vc.jwt.demos

import cats.implicits.*
import io.circe.*
import net.reactivecore.cjs.resolver.Downloader
import net.reactivecore.cjs.{DocumentValidator, Loader, Result}
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import java.security.*
import java.security.spec.*
import java.time.Instant

@main def schemaDemo(): Unit =
  val schemaCode =
    """
      |{
      |  "type": "object",
      |  "properties": {
      |    "userName": {
      |      "$ref": "#/$defs/user"
      |    },
      |    "age": {
      |      "$ref": "#/$defs/age"
      |    },
      |    "email": {
      |      "$ref": "#/$defs/email"
      |    }
      |  },
      |  "required": ["userName", "age", "email"],
      |  "$defs": {
      |    "user": {
      |       "type": "string",
      |       "minLength": 3
      |     },
      |     "age": {
      |       "type": "number"
      |     },
      |     "email": {
      |       "type": "string",
      |       "format": "email"
      |     }
      |  }
      |}
      |""".stripMargin

  val validator = Loader.empty.fromJson(schemaCode)

  def test(s: Json): Unit = {
    val result = validator.right.get.validate(s)
    println(s"Result of ${s}: ${result}")
  }

  test(Json.fromString("wrongType"))
  test(
    Json.obj(
      "userName" -> Json.fromString("Bob"),
      "age" -> Json.fromInt(42)
    )
  )

  // Missing UserName
  test(
    Json.obj(
      "age" -> Json.fromInt(42),
      "email" -> Json.fromString("email@email.com")
    )
  )

  // Age has Wrong type
  test(
    Json.obj(
      "userName" -> Json.fromString("Bob"),
      "age" -> Json.fromBoolean(false),
      "email" -> Json.fromString("email@email.com")
    )
  )

  // Success
  test(
    Json.obj(
      "userName" -> Json.fromString("Bob"),
      "age" -> Json.fromInt(42),
      "email" -> Json.fromString("email")
    )
  )
