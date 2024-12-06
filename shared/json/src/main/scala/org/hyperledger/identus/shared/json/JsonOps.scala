package org.hyperledger.identus.shared.json

import zio.json.ast.Json as ZioJson

object JsonOps {
  extension (json: ZioJson) {
    def removeNullValues: ZioJson = json match
      case ZioJson.Obj(fields) =>
        ZioJson.Obj(fields.collect { case (key, value) if value != ZioJson.Null => key -> value.removeNullValues })
      case ZioJson.Arr(elements) =>
        ZioJson.Arr(elements.map(_.removeNullValues))
      case other => other

    def removeField(name: String): ZioJson = json match
      case ZioJson.Obj(fields) =>
        ZioJson.Obj(fields.filterNot { case (key, value) => key == name })
      case ZioJson.Arr(elements) =>
        ZioJson.Arr(elements.map(_.removeField(name)))
      case other => other
  }
}
