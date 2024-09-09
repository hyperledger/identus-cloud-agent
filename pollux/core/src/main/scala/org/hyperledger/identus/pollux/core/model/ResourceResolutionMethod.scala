package org.hyperledger.identus.pollux.core.model

import zio.json.{JsonDecoder, JsonEncoder}

enum ResourceResolutionMethod(val str: String) {
  case DID extends ResourceResolutionMethod("did")
  case HTTP extends ResourceResolutionMethod("http")
}

object ResourceResolutionMethod {
  given JsonEncoder[ResourceResolutionMethod] = JsonEncoder[String].contramap[ResourceResolutionMethod](_.str)

  given JsonDecoder[ResourceResolutionMethod] = JsonDecoder[String].mapOrFail {
    case "did"  => Right(ResourceResolutionMethod.DID)
    case "http" => Right(ResourceResolutionMethod.HTTP)
    case other  => Left(s"Unknown ResourceResolutionMethod: $other")
  }
}
