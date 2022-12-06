package io.iohk.atala.pollux.vc.jwt.demos

import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.pollux.vc.jwt.CredentialPayload.Implicits.*
import io.iohk.atala.pollux.vc.jwt.PresentationPayload.Implicits.*
import net.reactivecore.cjs.resolver.Downloader
import net.reactivecore.cjs.{DocumentValidator, Loader, Result}
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import java.security.*
import java.security.spec.*
import java.time.{Instant, ZonedDateTime}

@main def JwtPresentationWithJWTCredentialDemo(): Unit =

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
  val w3cCredentialPayload =
    W3cCredentialPayload(
      `@context` = Set("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
      maybeId = Some("http://example.edu/credentials/3732"),
      `type` = Set("VerifiableCredential", "UniversityDegreeCredential"),
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

  val w3cIssuerSignedCredential = issuer.signer.encode(w3cCredentialPayload.asJson)
  val w3cVerifiableCredentialPayload =
    W3cVerifiableCredentialPayload(
      payload = w3cCredentialPayload,
      proof = Proof(
        `type` = "JwtProof2020",
        jwt = w3cIssuerSignedCredential
      )
    )

  val JWTIssuerSignedCredential = issuer.signer.encode(w3cCredentialPayload.toJwtCredentialPayload.asJson)
  val jwtVerifiableCredentialPayload = JwtVerifiableCredentialPayload(JWTIssuerSignedCredential)

  val w3cPresentationPayload =
    W3cPresentationPayload(
      `@context` =
        Vector("https://www.w3.org/2018/presentations/v1", "https://www.w3.org/2018/presentations/examples/v1"),
      maybeId = Some("http://example.edu/presentations/3732"),
      `type` = Vector("VerifiablePresentation", "UniversityDegreePresentation"),
      verifiableCredential = Vector(w3cVerifiableCredentialPayload, jwtVerifiableCredentialPayload),
      holder = "https://example.edu/holder/565049",
      verifier = Vector("https://example.edu/issuers/565049"),
      maybeIssuanceDate = Some(Instant.parse("2010-01-01T00:00:00Z")),
      maybeExpirationDate = Some(Instant.parse("2010-01-12T00:00:00Z"))
    )
  println(w3cPresentationPayload.asJson.toString())

  println("")
  println("==================")
  println("W3C Presentation => Encoded JWT")
  println("==================")
  val encodedJWT = JwtPresentation.toEncodedJwt(w3cPresentationPayload, holder)
  println(encodedJWT)

  println("")
  println("==================")
  println("Validate Encoded JWT")
  println("==================")
  val valid = JwtPresentation.validateEncodedJwt(encodedJWT, holder.publicKey)
  println(s"Is Valid? $valid")

  println("")
  println("==================")
  println("Encoded JWT => Decoded JWT Presentation Json")
  println("==================")
  val decodedJwtPresentation = JwtPresentation.decodeJwt(encodedJWT, holder.publicKey).toOption.get
  val decodedJwtPresentationAsJson = decodedJwtPresentation.asJson.toString()
  println(decodedJwtPresentationAsJson)

  println("")
  println("==================")
  println("Validates Signature Of Credentials")
  println("==================")
  decodedJwtPresentation.vp.verifiableCredential.foreach {
    case (w3cVerifiableCredentialPayload: W3cVerifiableCredentialPayload) =>
      println(s"w3cVerifiableCredentialPayload Is Valid? ${JwtPresentation
          .validateEncodedJwt(w3cVerifiableCredentialPayload.proof.jwt, issuer.publicKey)}")
    case (jwtVerifiableCredentialPayload: JwtVerifiableCredentialPayload) =>
      println(s"jwtVerifiableCredentialPayload Is Valid? ${JwtPresentation
          .validateEncodedJwt(jwtVerifiableCredentialPayload.jwt, issuer.publicKey)}")
  }

  println("")
  println("==================")
  println("W3C Presentation => JWT Presentation Json")
  println("==================")
  val jwtPresentationPayload = w3cPresentationPayload.toJwtPresentationPayload
  val jwtPresentationPayloadAsJson = jwtPresentationPayload.asJson.toString()
  println(jwtPresentationPayloadAsJson)

  println("")
  println("==================")
  println("JWT Presentation Json = Decoded JWT Presentation Json")
  println("==================")
  println(s"Are equal? ${jwtPresentationPayloadAsJson == decodedJwtPresentationAsJson}")
