package io.iohk.atala.pollux.vc.jwt.demos
import io.iohk.atala.pollux.vc.jwt.{
  CredentialSchema,
  CredentialStatus,
  DID,
  JwtCredentialPayload,
  RefreshService,
  W3cCredentialPayload
}
import io.iohk.atala.pollux.vc.jwt.VerifiedCredentialJson.Encoders.Implicits.*
import io.iohk.atala.pollux.vc.jwt.VerifiedCredentialJson.Decoders.Implicits.*
import cats.implicits.*
import io.circe.*
import net.reactivecore.cjs.resolver.Downloader
import net.reactivecore.cjs.{DocumentValidator, Loader, Result}
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.parser.decode

import java.security.*
import java.security.spec.*
import java.time.{Instant, ZonedDateTime}

@main def CredentialDemo(): Unit =
  val w3cCredentialPayload = W3cCredentialPayload(
    `@context` = Vector("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
    maybeId = Some("http://example.edu/credentials/3732"),
    `type` = Vector("VerifiableCredential", "UniversityDegreeCredential"),
    issuer = DID("https://example.edu/issuers/565049"),
    issuanceDate = Instant.parse("2010-01-01T00:00:00Z"),
    maybeExpirationDate = Some(Instant.parse("2010-01-12T00:00:00Z")),
    maybeCredentialSchema = Some(
      CredentialSchema(
        id = "did:work:MDP8AsFhHzhwUvGNuYkX7T;id=06e126d1-fa44-4882-a243-1e326fbe21db;version=1.0",
        `type` = "JsonSchemaValidator2018"
      )
    ),
    credentialSubject = Json.obj(
      "userName" -> Json.fromString("Bob"),
      "age" -> Json.fromInt(42),
      "email" -> Json.fromString("email")
    ),
    maybeCredentialStatus = Some(
      CredentialStatus(
        id = "did:work:MDP8AsFhHzhwUvGNuYkX7T;id=06e126d1-fa44-4882-a243-1e326fbe21db;version=1.0",
        `type` = "CredentialStatusList2017"
      )
    ),
    maybeRefreshService = Some(
      RefreshService(
        id = "https://example.edu/refresh/3732",
        `type` = "ManualRefreshService2018"
      )
    ),
    maybeEvidence = Option.empty,
    maybeTermsOfUse = Option.empty
  )

  println("")
  println("==================")
  println("W3C => W3C Json")
  println("==================")
  val w3cJson = w3cCredentialPayload.asJson.toString()
  println(w3cJson)

  println("")
  println("==================")
  println("W3C Json => W3C")
  println("==================")
  val decodedW3CJson = decode[W3cCredentialPayload](w3cJson).toOption.get
  println(decodedW3CJson)

  println("")
  println("==================")
  println("W3C => JWT")
  println("==================")
  val jwtCredentialPayload = w3cCredentialPayload.toJwtCredentialPayload
  println(jwtCredentialPayload)

  println("")
  println("==================")
  println("JWT => JWT+AUD")
  println("==================")
  val jwtAudCredentialPayload =
    jwtCredentialPayload.copy(aud =
      Vector("did:example:4a57546973436f6f6c4a4a57573", "did:example:s7dfsd86f5sd6fsdf6sfs6d5sdf")
    )
  println(jwtAudCredentialPayload)

  println("")
  println("==================")
  println("JWT+AUD => JWT+AUD Json")
  println("==================")
  val jwtAudCredentialJson = jwtAudCredentialPayload.asJson.toString()
  println(jwtAudCredentialJson)

  println("")
  println("==================")
  println("JWT+AUD Json => JWT+AUD ")
  println("==================")
  val decodedJwtAudCredentialPayload = decode[JwtCredentialPayload](jwtAudCredentialJson).toOption.get
  println(decodedJwtAudCredentialPayload)

  println("")
  println("==================")
  println("JWT+AUD => W3C")
  println("==================")
  val convertedJwtAudToW3CCredential = decodedJwtAudCredentialPayload.toW3CCredentialPayload
  println(convertedJwtAudToW3CCredential.toOption.get.asJson.toString())

  println("")
  println("==================")
  println("W3C => JWT+AUD")
  println("==================")
  val convertedW3CToJwtAudCredential = convertedJwtAudToW3CCredential.toOption.get.toJwtCredentialPayload
  println(convertedW3CToJwtAudCredential.asJson.toString())
