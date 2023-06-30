package io.iohk.atala.pollux.vc.jwt.demos

import io.circe.*
import io.circe.syntax.*
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.pollux.vc.jwt.CredentialPayload.Implicits.*
import io.iohk.atala.pollux.vc.jwt.PresentationPayload.Implicits.*

import java.security.*
import java.security.spec.*
import java.time.*
import scala.collection.immutable.Set

@main def JwtPresentationTemporalVerificationDemo(): Unit =
  def createUser(did: DID) = {
    val keyGen = KeyPairGenerator.getInstance("EC")
    val ecSpec = ECGenParameterSpec("secp256r1")
    keyGen.initialize(ecSpec, SecureRandom())
    val keyPair = keyGen.generateKeyPair()
    val privateKey = keyPair.getPrivate
    val publicKey = keyPair.getPublic
    Issuer(
      did = did,
      signer = ES256Signer(privateKey),
      publicKey = publicKey
    )
  }

  println("")
  println("==================")
  println("Create Issuer")
  println("==================")
  val issuer =
    createUser(DID("did:issuer:MDP8AsFhHzhwUvGNuYkX7T"))
  println(issuer)

  println("")
  println("==================")
  println("Create Holder")
  println("==================")
  val holder =
    createUser(DID("did:holder:MDP8AsFhHzhwUvGNuYkX7T"))
  println(holder)

  println("")
  println("==================")
  println("Create W3C Presentation")
  println("==================")
  val w3cIssuanceDate = Instant.parse("2010-01-01T00:00:00Z") // ISSUANCE DATE
  val w3cExpirationDate = Instant.parse("2010-01-12T00:00:00Z") // EXPIRATION DATE
  val w3cCredentialPayload =
    W3cCredentialPayload(
      `@context` = Set("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
      maybeId = Some("http://example.edu/credentials/3732"),
      `type` = Set("VerifiableCredential", "UniversityDegreeCredential"),
      issuer = DID("https://example.edu/issuers/565049"),
      issuanceDate = w3cIssuanceDate,
      maybeExpirationDate = Some(w3cExpirationDate),
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

  val w3cIssuerSignedCredential = issuer.signer.encode(w3cCredentialPayload.asJson)
  val w3cVerifiableCredentialPayload =
    W3cVerifiableCredentialPayload(
      payload = w3cCredentialPayload,
      proof = Proof(
        `type` = "JwtProof2020",
        jwt = w3cIssuerSignedCredential
      )
    )

  val jwtCredentialNbf = Instant.parse("2010-01-01T00:00:00Z") // ISSUANCE DATE
  val jwtCredentialExp = Instant.parse("2010-01-12T00:00:00Z") // EXPIRATION DATE
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
      nbf = jwtCredentialNbf, // ISSUANCE DATE
      aud = Set.empty,
      maybeExp = Some(jwtCredentialExp), // EXPIRATION DATE
      maybeJti = Some("http://example.edu/credentials/3732") // CREDENTIAL ID
    )

  val JWTIssuerSignedCredential = issuer.signer.encode(jwtCredentialPayload.asJson)
  val jwtVerifiableCredentialPayload = JwtVerifiableCredentialPayload(JWTIssuerSignedCredential)

  val jwtPresentationNbf = Instant.parse("2010-01-01T00:00:00Z") // ISSUANCE DATE
  val jwtPresentationExp = Instant.parse("2010-01-12T00:00:00Z") // EXPIRATION DATE

  def presentationPayload(
      maybeJwtPresentationNbf: Option[Instant],
      maybeJwtPresentationExp: Option[Instant]
  ): JwtPresentationPayload = {
    JwtPresentationPayload(
      iss = "https://example.edu/holder/565049",
      maybeJti = Some("http://example.edu/presentations/3732"),
      vp = JwtVp(
        `@context` =
          Vector("https://www.w3.org/2018/presentations/v1", "https://www.w3.org/2018/presentations/examples/v1"),
        `type` = Vector("VerifiablePresentation", "UniversityDegreePresentation"),
        verifiableCredential = Vector(w3cVerifiableCredentialPayload, jwtVerifiableCredentialPayload)
      ),
      aud = Vector("https://example.edu/issuers/565049"),
      maybeNbf = maybeJwtPresentationNbf,
      maybeExp = maybeJwtPresentationExp,
      maybeNonce = None
    )
  }

  val jwtPresentationPayload = presentationPayload(Some(jwtPresentationNbf), Some(jwtPresentationExp))
  println(jwtPresentationPayload.asJson.toString())

  println("")
  println("==================")
  println("Encoded JWT")
  println("==================")
  val encodedJWTPresentation = JwtPresentation.encodeJwt(payload = jwtPresentationPayload, issuer = issuer)
  println(encodedJWTPresentation)

  println("")
  println("==================")
  println("Validate JWT between ISSUANCE DATE and EXPIRATION DATE")
  println("==================")
  val clockWithCurrentTime = Clock.fixed(jwtPresentationNbf.plus(Duration.ofDays(1)), ZoneId.systemDefault)
  val validAtCurrentTime =
    JwtPresentation.verifyDates(jwt = encodedJWTPresentation, leeway = Duration.ZERO)(clock = clockWithCurrentTime)
  println(s"Is Valid at current time? $validAtCurrentTime")

  println("")
  println("==================")
  println("Validate JWT on ISSUANCE DATE")
  println("==================")
  val clockWithFixedTimeAtNbf = Clock.fixed(jwtPresentationNbf, ZoneId.systemDefault)
  val validAtNbf =
    JwtPresentation.verifyDates(jwt = encodedJWTPresentation, leeway = Duration.ZERO)(clock = clockWithFixedTimeAtNbf)
  println(s"Is Valid at ISSUANCE DATE? $validAtNbf")

  println("")
  println("==================")
  println("Validate JWT on EXPIRATION DATE")
  println("==================")
  val clockWithFixedTimeAtExp = Clock.fixed(jwtPresentationExp, ZoneId.systemDefault)
  val validAtExp =
    JwtPresentation.verifyDates(jwt = encodedJWTPresentation, leeway = Duration.ZERO)(clock = clockWithFixedTimeAtExp)
  println(s"Is Valid at EXPIRATION DATE? $validAtExp")

  println("")
  println("==================")
  println("Validate JWT before ISSUANCE DATE")
  println("==================")
  val clockWithFixedTimeBeforeNbf = Clock.fixed(jwtPresentationNbf.minus(Duration.ofDays(1)), ZoneId.systemDefault)
  val validBeforeNbf =
    JwtPresentation.verifyDates(jwt = encodedJWTPresentation, leeway = Duration.ZERO)(clock =
      clockWithFixedTimeBeforeNbf
    )
  println(s"Is Valid before ISSUANCE DATE? $validBeforeNbf")

  println("")
  println("==================")
  println("Validate JWT after EXPIRATION DATE")
  println("==================")
  val clockWithFixedTimeAfterExp = Clock.fixed(jwtPresentationExp.plus(Duration.ofDays(1)), ZoneId.systemDefault)
  val validAfterExp =
    JwtPresentation.verifyDates(jwt = encodedJWTPresentation, leeway = Duration.ZERO)(clock =
      clockWithFixedTimeAfterExp
    )
  println(s"Is Valid after EXPIRATION DATE? $validAfterExp")

  println("")
  println("==================")
  println("Validate JWT before ISSUANCE DATE with 1 Day Leeway")
  println("==================")
  val leeway = Duration.ofDays(1)
  val validBeforeNbfWithLeeway =
    JwtPresentation.verifyDates(jwt = encodedJWTPresentation, leeway = leeway)(clock = clockWithFixedTimeBeforeNbf)
  println(
    s"Is Valid before ISSUANCE DATE with 1 Day Leeway? $validBeforeNbfWithLeeway with leeway:$leeway"
  )

  println("")
  println("==================")
  println("Validate JWT after EXPIRATION DATE with 1 Day Leeway")
  println("==================")
  val validAfterExpWithLeeway =
    JwtPresentation.verifyDates(jwt = encodedJWTPresentation, leeway = leeway)(clock = clockWithFixedTimeAfterExp)
  println(s"Is Valid after EXPIRATION DATE with 1 Day Leeway? $validAfterExpWithLeeway with leeway:$leeway")

  println("")
  println("==================")
  println("Validate JWT No ISSUANCE DATE")
  println("==================")
  val encodedJWTPresentationNoNB =
    JwtPresentation.encodeJwt(payload = presentationPayload(None, Some(jwtPresentationExp)), issuer = issuer)

  val noNbfValidBeforeExpiration =
    JwtPresentation.verifyDates(jwt = encodedJWTPresentationNoNB, leeway = Duration.ZERO)(clock = clockWithCurrentTime)
  println(s"Is Valid before EXPIRATION DATE: $noNbfValidBeforeExpiration")

  println("")
  println("==================")
  println("Validated JWT No EXPIRATION DATE")
  println("==================")
  val encodedJWTPresentationNoEXP =
    JwtPresentation.encodeJwt(payload = presentationPayload(Some(jwtPresentationNbf), None), issuer = issuer)
  val noExpValidAfterNbf =
    JwtPresentation.verifyDates(jwt = encodedJWTPresentationNoEXP, leeway = Duration.ZERO)(clock = clockWithCurrentTime)
  println(s"Is Valid after ISSUANCE DATE: $noExpValidAfterNbf")

  println("")
  println("==================")
  println("Validate JWT No EXPIRATION & No ISSUANCE DATE")
  println("==================")
  val encodedJWTPresentationNoEXPNoNbf: JWT =
    JwtPresentation.encodeJwt(payload = presentationPayload(None, None), issuer = issuer)
  val noEXPAndNbfAlwaysValid =
    JwtPresentation.verifyDates(jwt = encodedJWTPresentationNoEXPNoNbf, leeway = Duration.ZERO)(clock =
      clockWithCurrentTime
    )
  println(s"Is Always Valid With non EXPIRATION DATE & No ISSUANCE DATE: $noEXPAndNbfAlwaysValid")
