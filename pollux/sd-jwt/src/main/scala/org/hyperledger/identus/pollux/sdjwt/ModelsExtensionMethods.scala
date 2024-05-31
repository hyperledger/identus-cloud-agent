package org.hyperledger.identus.pollux.sdjwt

import zio.json._

import java.util.Base64

private[sdjwt] object ModelsExtensionMethods {
  extension (c: String) {
    private def asJsonObject: Either[String, ast.Json.Obj] = c
      .fromJson[ast.Json]
      .map(_.asObject)
      .flatMap {
        case None          => Left("PresentationJson must the a Json Object")
        case Some(jsonObj) => Right(jsonObj)
      }
    def payload: Either[String, String] =
      asJsonObject.flatMap {
        _.get("payload") match
          case None => Left("PresentationJson must have the field 'payload'")
          case Some(ast.Json.Str(payload)) =>
            Right(
              String(
                Base64.getDecoder().decode(payload) // TODO make it safe
              )
            )
          case Some(_) => Left("PresentationJson must have the field 'payload' as a Base64 String")
      }
    private def payloadAsJsonObj: Either[String, zio.json.ast.Json.Obj] =
      payload.flatMap {
        _.fromJson[ast.Json]
          .map(_.asObject)
          .flatMap {
            case None          => Left("The payload in PresentationJson must the a Json Object")
            case Some(jsonObj) => Right(jsonObj)
          }
      }

    def iss: Either[String, String] =
      payloadAsJsonObj.flatMap {
        _.get("iss") match
          case None                    => Left("The payload in PresentationJson must have the field 'iss'")
          case Some(ast.Json.Str(iss)) => Right(iss)
          case Some(_)                 => Left("PresentationJson must have the field 'iss' as a String")
      }

    def sub: Either[String, String] =
      payloadAsJsonObj.flatMap {
        _.get("sub") match
          case None                    => Left("The payload in PresentationJson must have the field 'sub'")
          case Some(ast.Json.Str(sub)) => Right(sub)
          case Some(_)                 => Left("PresentationJson must have the field 'sub' as a String")
      }

    def iat: Either[String, BigDecimal] =
      payloadAsJsonObj.flatMap {
        _.get("iat") match
          case None                    => Left("The payload in PresentationJson must have the field 'iat'")
          case Some(ast.Json.Num(iat)) => Right(iat)
          case Some(_)                 => Left("PresentationJson must have the field 'iat' as a Num")
      }

    def exp: Either[String, BigDecimal] =
      payloadAsJsonObj.flatMap {
        _.get("exp") match
          case None                    => Left("The payload in PresentationJson must have the field 'exp'")
          case Some(ast.Json.Num(exp)) => Right(exp)
          case Some(_)                 => Left("PresentationJson must have the field 'exp' as a Num")
      }
  }
}
