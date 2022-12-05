package io.iohk.atala.pollux.vc.jwt.demos

import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.pollux.vc.jwt.CredentialPayload.Implicits.*
import net.reactivecore.cjs.resolver.Downloader
import net.reactivecore.cjs.{DocumentValidator, Loader, Result}
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import java.security.*
import java.security.spec.*
import java.time.*

@main def JwtCredentialTemporalVerificationDemo(): Unit =

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
  println("Create JWT Credential")
  println("==================")
  val nbf = Instant.parse("2010-01-01T00:00:00Z")
  val exp = Instant.parse("2010-01-12T00:00:00Z")
  val jwtCredentialPayload =
    JwtCredentialPayload(
      iss = "https://example.edu/issuers/565049", // ISSUER DID
      maybeSub = Some("1"), // SUBJECT DID
      vc = JwtVc(
        `@context` = Set("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
        `type` = Set("VerifiableCredential", "UniversityDegreeCredential"),
        maybeCredentialSchema = Some(
          CredentialSchema(
            id = "did:work:MDP8AsFhHzhwUvGNuYkX7T;id=06e126d1-fa44-4882-a243-1e326fbe21db;version=1.0",
            `type` = "JsonSchemaValidator2018"
          )
        ),
        credentialSubject = Json.obj(
          "id" -> Json.fromString("1"),
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
      ),
      nbf = nbf, // ISSUANCE DATE
      aud = Set.empty,
      maybeExp = Some(exp), // EXPIRATION DATE
      maybeJti = Some("http://example.edu/credentials/3732") // CREDENTIAL ID
    )
  println(jwtCredentialPayload.asJson.toString())

  println("")
  println("==================")
  println("Encoded JWT")
  println("==================")
  val encodedJWT = JwtCredential.encodeJwt(payload = jwtCredentialPayload, issuer = issuer)
  println(encodedJWT)

  println("")
  println("==================")
  println("Validate JWT between nbf and exp")
  println("==================")
  val clockWithCurrentTime = Clock.fixed(nbf.plus(Duration.ofDays(1)), ZoneId.systemDefault)
  val validAtCurrentTime =
    JwtCredential.validateEncodedJWT(jwt = encodedJWT, leeway = Duration.ZERO)(clock = clockWithCurrentTime)
  println(s"Is Valid at current time? $validAtCurrentTime")

  println("")
  println("==================")
  println("Validate JWT on NBF")
  println("==================")
  val clockWithFixedTimeAtNbf = Clock.fixed(nbf, ZoneId.systemDefault)
  val validAtNbf =
    JwtCredential.validateEncodedJWT(jwt = encodedJWT, leeway = Duration.ZERO)(clock = clockWithFixedTimeAtNbf)
  println(s"Is Valid at NBF time? $validAtNbf")

  println("")
  println("==================")
  println("Validate JWT on EXP")
  println("==================")
  val clockWithFixedTimeAtExp = Clock.fixed(exp, ZoneId.systemDefault)
  val validAtExp =
    JwtCredential.validateEncodedJWT(jwt = encodedJWT, leeway = Duration.ZERO)(clock = clockWithFixedTimeAtExp)
  println(s"Is Valid at Exp time? $validAtExp")

  println("")
  println("==================")
  println("Validate JWT before NBF")
  println("==================")
  val clockWithFixedTimeBeforeNbf = Clock.fixed(nbf.minus(Duration.ofDays(1)), ZoneId.systemDefault)
  val validBeforeNbf =
    JwtCredential.validateEncodedJWT(jwt = encodedJWT, leeway = Duration.ZERO)(clock = clockWithFixedTimeBeforeNbf)
  println(s"Is Valid before NBF time? $validBeforeNbf")

  println("")
  println("==================")
  println("Validate JWT after EXP")
  println("==================")
  val clockWithFixedTimeAfterExp = Clock.fixed(exp.plus(Duration.ofDays(1)), ZoneId.systemDefault)
  val validAfterExp =
    JwtCredential.validateEncodedJWT(jwt = encodedJWT, leeway = Duration.ZERO)(clock = clockWithFixedTimeAfterExp)
  println(s"Is Valid after EXP time? $validAfterExp")

  println("")
  println("==================")
  println("Validate JWT before NBF with 1 Day Leeway")
  println("==================")
  val validBeforeNbfWithLeeway =
    JwtCredential.validateEncodedJWT(jwt = encodedJWT, leeway = Duration.ofDays(1))(clock = clockWithFixedTimeBeforeNbf)
  println(s"Is Valid before NBF time with 1 Day Leeway? $validBeforeNbfWithLeeway")

  println("")
  println("==================")
  println("Validate JWT after EXP with 1 Day Leeway")
  println("==================")
  val validAfterExpWithLeeway =
    JwtCredential.validateEncodedJWT(jwt = encodedJWT, leeway = Duration.ofDays(1))(clock = clockWithFixedTimeAfterExp)
  println(s"Is Valid after EXP time with 1 Day Leeway? $validAfterExpWithLeeway")
