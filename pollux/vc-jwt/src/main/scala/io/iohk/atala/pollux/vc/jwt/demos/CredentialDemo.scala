package io.iohk.atala.pollux.vc.jwt.demos
import io.iohk.atala.pollux.vc.jwt.{CredentialSchema, CredentialStatus, IssuerDID, RefreshService, W3CCredentialPayload}
import io.iohk.atala.pollux.vc.jwt.VerifiedCredentialJson.Encoders.Implicits._
import cats.implicits.*
import io.circe.*
import net.reactivecore.cjs.resolver.Downloader
import net.reactivecore.cjs.{DocumentValidator, Loader, Result}
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import io.circe.generic.auto._, io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}

import java.security.*
import java.security.spec.*
import java.time.{Instant, ZonedDateTime}

@main def CredentialDemo(): Unit =
  val w3cCredentialPayload = W3CCredentialPayload(
    `@context` = Vector("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
    id = Some("http://example.edu/credentials/3732"),
    `type` = Vector("VerifiableCredential", "UniversityDegreeCredential"),
    issuer = IssuerDID(id = "https://example.edu/issuers/565049"),
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
  println("W3C")
  println("==================")
  println(w3cCredentialPayload.asJson.toString())

  println("")
  println("==================")
  println("W3C => JWT")
  println("==================")
  val jwtCredentialPayload = w3cCredentialPayload.toJwtCredentialPayload
  println(jwtCredentialPayload.asJson.toString())

  println("")
  println("==================")
  println("JWT => W3C")
  println("==================")
  jwtCredentialPayload.toW3CCredentialPayload.foreach(payload => println(payload.asJson.toString()))
