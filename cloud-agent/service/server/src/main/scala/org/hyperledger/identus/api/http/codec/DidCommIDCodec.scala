package org.hyperledger.identus.api.http.codec

import sttp.tapir._
import sttp.tapir.Codec.PlainCodec
import org.hyperledger.identus.pollux.core.model.DidCommID
import sttp.tapir.DecodeResult.*

object DidCommIDCodec {
  given didCommIDCodec: PlainCodec[DidCommID] =
    Codec.string.mapDecode { s =>
      if s.nonEmpty && s.length < 64 then Value(DidCommID(s))
      else
        Error(
          "DidComId must be less then 64 characters long",
          new Throwable("DidComId must be less then 64 characters long")
        )
    }(didCommID => didCommID.value)

}
