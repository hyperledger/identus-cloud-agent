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
  val nbf = Instant.parse("2010-01-01T00:00:00Z") // ISSUANCE DATE
  val exp = Instant.parse("2010-01-12T00:00:00Z") // EXPIRATION DATE
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
  println("Validate JWT between ISSUANCE DATE and EXPIRATION DATE")
  println("==================")
  val clockWithCurrentTime = Clock.fixed(nbf.plus(Duration.ofDays(1)), ZoneId.systemDefault)
  val validAtCurrentTime =
    JwtCredential.verifyDates(jwt = encodedJWT, leeway = Duration.ZERO)(clock = clockWithCurrentTime)
  println(s"Is Valid at current time? $validAtCurrentTime")

  println("")
  println("==================")
  println("Validate JWT on ISSUANCE DATE")
  println("==================")
  val clockWithFixedTimeAtNbf = Clock.fixed(nbf, ZoneId.systemDefault)
  val validAtNbf =
    JwtCredential.verifyDates(jwt = encodedJWT, leeway = Duration.ZERO)(clock = clockWithFixedTimeAtNbf)
  println(s"Is Valid at ISSUANCE DATE? $validAtNbf")

  println("")
  println("==================")
  println("Validate JWT on EXPIRATION DATE")
  println("==================")
  val clockWithFixedTimeAtExp = Clock.fixed(exp, ZoneId.systemDefault)
  val validAtExp =
    JwtCredential.verifyDates(jwt = encodedJWT, leeway = Duration.ZERO)(clock = clockWithFixedTimeAtExp)
  println(s"Is Valid at EXPIRATION DATE? $validAtExp")

  println("")
  println("==================")
  println("Validate JWT before ISSUANCE DATE")
  println("==================")
  val clockWithFixedTimeBeforeNbf = Clock.fixed(nbf.minus(Duration.ofDays(1)), ZoneId.systemDefault)
  val validBeforeNbf =
    JwtCredential.verifyDates(jwt = encodedJWT, leeway = Duration.ZERO)(clock = clockWithFixedTimeBeforeNbf)
  println(s"Is Valid before ISSUANCE DATE? $validBeforeNbf")

  println("")
  println("==================")
  println("Validate JWT after EXPIRATION DATE")
  println("==================")
  val clockWithFixedTimeAfterExp = Clock.fixed(exp.plus(Duration.ofDays(1)), ZoneId.systemDefault)
  val validAfterExp =
    JwtCredential.verifyDates(jwt = encodedJWT, leeway = Duration.ZERO)(clock = clockWithFixedTimeAfterExp)
  println(s"Is Valid after EXPIRATION DATE? $validAfterExp")

  println("")
  println("==================")
  println("Validate JWT before ISSUANCE DATE with 1 Day Leeway")
  println("==================")
  val leeway = Duration.ofDays(1)
  val validBeforeNbfWithLeeway =
    JwtCredential.verifyDates(jwt = encodedJWT, leeway = leeway)(clock = clockWithFixedTimeBeforeNbf)
  println(
    s"Is Valid before ISSUANCE DATE with 1 Day Leeway? $validBeforeNbfWithLeeway with leeway:$leeway"
  )

  println("")
  println("==================")
  println("Validate JWT after EXPIRATION DATE with 1 Day Leeway")
  println("==================")
  val validAfterExpWithLeeway =
    JwtCredential.verifyDates(jwt = encodedJWT, leeway = leeway)(clock = clockWithFixedTimeAfterExp)
  println(s"Is Valid after EXPIRATION DATE with 1 Day Leeway? $validAfterExpWithLeeway with leeway:$leeway")
