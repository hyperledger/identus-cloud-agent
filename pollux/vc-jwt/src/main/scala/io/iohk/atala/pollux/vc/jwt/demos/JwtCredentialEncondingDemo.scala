package io.iohk.atala.pollux.vc.jwt.demos

import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.pollux.vc.jwt.VerifiedCredentialJson.Encoders.Implicits.*
import net.reactivecore.cjs.resolver.Downloader
import net.reactivecore.cjs.{DocumentValidator, Loader, Result}
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import java.security.*
import java.security.spec.*
import java.time.{Instant, ZonedDateTime}

@main def JwtCredentialEncondingDemo(): Unit =

  println("")
  println("==================")
  println("Create Issuer")
  println("==================")
  val keyGen = KeyPairGenerator.getInstance("EC")
  val ecSpec = ECGenParameterSpec("secp256r1")
  keyGen.initialize(ecSpec, SecureRandom())
  val keyPair = keyGen.generateKeyPair()
  val privateKey = keyPair.getPrivate
  val publicKey = keyPair.getPublic
  val issuer =
    Issuer(
      did = DID("did:issuer:MDP8AsFhHzhwUvGNuYkX7T"),
      signer = ES256Signer(privateKey),
      publicKey = publicKey
    )
  println(issuer)

  println("")
  println("==================")
  println("Create W3C")
  println("==================")
  val w3cCredentialPayload = W3CCredentialPayload(
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
  println(w3cCredentialPayload.asJson.toString())

  println("")
  println("==================")
  println("W3C => Encoded JWT")
  println("==================")
  val encodedJWT = JwtVerifiableCredential.toEncodedJwt(w3cCredentialPayload, issuer)
  println(encodedJWT)

  println("")
  println("==================")
  println("Validate Encoded JWT")
  println("==================")
  val valid = JwtVerifiableCredential.validateEncodedJwt(encodedJWT, issuer.publicKey)
  println(s"Is Valid? $valid")

  println("")
  println("==================")
  println("Encoded JWT => Decoded JWT Json")
  println("==================")
  val decodedJwtCredential = JwtVerifiableCredential.decodeJwt(encodedJWT, issuer.publicKey)
  val decodedJwtCredentialAsJson = decodedJwtCredential.toOption.get.asJson.toString()
  println(decodedJwtCredentialAsJson)

  println("")
  println("==================")
  println("W3C => JWT Json")
  println("==================")
  val jwtCredentialPayload = w3cCredentialPayload.toJwtCredentialPayload
  val jwtCredentialPayloadAsJson = jwtCredentialPayload.asJson.toString()
  println(jwtCredentialPayloadAsJson)

  println("")
  println("==================")
  println("JWT Json = Decoded JWT Json")
  println("==================")
  println(s"Are equal? ${jwtCredentialPayloadAsJson == decodedJwtCredentialAsJson}")
