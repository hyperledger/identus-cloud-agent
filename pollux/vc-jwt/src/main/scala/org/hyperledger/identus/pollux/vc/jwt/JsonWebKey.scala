package org.hyperledger.identus.pollux.vc.jwt

import io.circe.*
import io.circe.syntax.*

case class JsonWebKey(
    alg: Option[String] = Option.empty,
    crv: Option[String] = Option.empty,
    e: Option[String] = Option.empty,
    d: Option[String] = Option.empty,
    ext: Option[Boolean] = Option.empty,
    key_ops: Vector[String] = Vector.empty,
    kid: Option[String] = Option.empty,
    kty: String,
    n: Option[String] = Option.empty,
    use: Option[String] = Option.empty,
    x: Option[String] = Option.empty,
    y: Option[String] = Option.empty
)

object JsonWebKey {
  given jsonWebKeyEncoder: Encoder[JsonWebKey] =
    (jsonWebKey: JsonWebKey) =>
      Json
        .obj(
          ("alg", jsonWebKey.alg.asJson),
          ("crv", jsonWebKey.crv.asJson),
          ("e", jsonWebKey.e.asJson),
          ("d", jsonWebKey.d.asJson),
          ("ext", jsonWebKey.ext.asJson),
          ("key_ops", jsonWebKey.key_ops.asJson),
          ("kid", jsonWebKey.kid.asJson),
          ("kty", jsonWebKey.kty.asJson),
          ("n", jsonWebKey.n.asJson),
          ("use", jsonWebKey.use.asJson),
          ("x", jsonWebKey.x.asJson),
          ("y", jsonWebKey.y.asJson),
        )

  given jsonWebKeyDecoder: Decoder[JsonWebKey] =
    (c: HCursor) =>
      for {
        alg <- c.downField("alg").as[Option[String]]
        crv <- c.downField("crv").as[Option[String]]
        e <- c.downField("e").as[Option[String]]
        d <- c.downField("d").as[Option[String]]
        ext <- c.downField("ext").as[Option[Boolean]]
        key_ops <- c.downField("key_ops").as[Vector[String]]
        kid <- c.downField("kid").as[Option[String]]
        kty <- c.downField("kty").as[String]
        n <- c.downField("n").as[Option[String]]
        use <- c.downField("use").as[Option[String]]
        x <- c.downField("x").as[Option[String]]
        y <- c.downField("y").as[Option[String]]
      } yield {
        JsonWebKey(
          alg = alg,
          crv = crv,
          e = e,
          d = d,
          ext = ext,
          key_ops = key_ops,
          kid = kid,
          kty = kty,
          n = n,
          use = use,
          x = x,
          y = y
        )
      }

}
