package io.iohk.atala.mercury.model

import scala.jdk.CollectionConverters._

import io.circe._
import io.circe.parser._

object JsonUtilsForDidCommx {
  private type JsonValue = Boolean | JsonNumber | String | Json.Null.type
  private type SubJsonFields[F] = Seq[F] | Map[String, F]
  private type JsonRecursiveType[F] = SubJsonFields[F] | JsonValue
  case class FixJson[F[_]](out: F[FixJson[F]])
  type MyJson = FixJson[JsonRecursiveType]
  type MyJsonTop = Map[String, MyJson]

  private def fromJson(body: JsonObject): MyJsonTop =
    body.toMap.map { case (k, v) => (k, fromJsonAux(v)) }

  private def fromJsonAux(body: Json): MyJson = body.fold[MyJson](
    jsonNull = FixJson(Json.Null),
    jsonBoolean = b => FixJson(b),
    jsonNumber = n => FixJson(n), // TODO JsonNumber,
    jsonString = s => FixJson(s),
    jsonArray = v => FixJson(v.toSeq.map(json => fromJsonAux(json))),
    jsonObject = m => FixJson(m.toMap.map { case (k, v) => (k, fromJsonAux(v)) }),
  )

  def fromJsonToJavaMap(body: JsonObject) = toJavaMap(fromJson(body))

  private def toJavaMap(json: MyJsonTop): java.util.Map[String, Any] = {
    def myJsonToMap(f: MyJson): Any = f match {
      case FixJson(Json.Null) => null // TODO Json.Null ->  An illegal reflective access operation has occurred
      case FixJson(jsonValue: JsonValue)              => jsonValue
      case FixJson(seq: Seq[MyJson] @unchecked)       => seq.map(myJsonToMap(_)).toArray
      case FixJson(m: Map[String, MyJson] @unchecked) => toJavaMap(m)
    }
    json.map { case (s, fixJson) => (s, myJsonToMap(fixJson)) }.asJava
  }

  // def toMap(json: MyJsonTop): Map[String, Any] = {
  //   def myJsonToMap(f: MyJson): Any = f match {
  //     case FixJson(jsonValue: JsonValue)              => jsonValue
  //     case FixJson(seq: Seq[MyJson] @unchecked)       => seq.map(myJsonToMap(_))
  //     case FixJson(m: Map[String, MyJson] @unchecked) => toJavaMap(m)
  //   }
  //   json.map { case (s, fixJson) => (s, myJsonToMap(fixJson)) }
  // }

  def toJson(json: MyJsonTop): JsonObject = {
    def myJsonToJson(f: MyJson): Json = f match {
      case FixJson(jsonValue: Json.Null.type)   => Json.Null // TEST
      case FixJson(jsonValue: Boolean)          => Json.fromBoolean(jsonValue)
      case FixJson(jsonValue: JsonNumber)       => Json.fromJsonNumber(jsonValue)
      case FixJson(jsonValue: String)           => Json.fromString(jsonValue)
      case FixJson(seq: Seq[MyJson] @unchecked) => Json.fromValues(seq.map(myJsonToJson(_)))
      case FixJson(m: Map[String, MyJson] @unchecked) =>
        Json.fromJsonObject(
          JsonObject.fromMap(m.map { case (s, fixJson) => (s, myJsonToJson(fixJson)) })
        )
    }
    JsonObject.fromMap(json.map { case (s, fixJson) => (s, myJsonToJson(fixJson)) })
  }

  def fromJavaMapToJsonAux(m: java.util.Map[String, Any]): MyJsonTop = {
    def auxMethod(f: Any): MyJson = f match {
      case a: com.nimbusds.jose.shaded.json.JSONArray => // TODO why do we need this
        FixJson(a.toArray.toSeq.map(e => auxMethod(e)))
      case null                                     => FixJson(Json.Null) // Can be remove if we use JsonNull
      case a: Array[Any]                            => FixJson(a.toSeq.map(e => auxMethod(e)))
      case e: java.util.Map[String, Any] @unchecked => FixJson(fromJavaMapToJsonAux(e))
      case value: JsonValue                         => FixJson(value)
    }
    m.asScala.toMap.map { case (s, any) => (s: String, auxMethod(any): MyJson) }
  }

  def fromJavaMapToJson(m: java.util.Map[String, Any]): JsonObject = toJson(fromJavaMapToJsonAux(m))
}

@main def JsonUtilsForDidCommxMain() = {
  val json = """
    |{
    |  "a" : {"b": {"c": 1.1}},
    |  "type": "DIDCommMessaging",
    |  "serviceEndpoint": "http://localhost:8000/",
    |  "routingKeys": ["did:example:somemediator#somekey2"],
    |  "accept": ["didcomm/v2", "didcomm/aip2;env=rfc587"]
    |}""".stripMargin

  val x1 = parse(json).toOption.flatMap(_.asObject).get
  println(x1)

  val x2 = JsonUtilsForDidCommx.fromJsonToJavaMap(x1)
  println(x2)

  val x3 = JsonUtilsForDidCommx.fromJavaMapToJsonAux(x2)
  println(x3)

  // val x4 = JsonUtilsForDidCommx.toMap(x3)
  // println(x4)

  val x5 = JsonUtilsForDidCommx.toJson(x3)
  println(x5)

}
