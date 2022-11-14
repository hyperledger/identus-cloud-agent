package io.iohk.atala.pollux.vc.jwt.demos

import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.pollux.vc.jwt.CredentialPayload.Implicits.*
import io.iohk.atala.pollux.vc.jwt.schema.{PlaceholderSchemaValidator, ReactiveCoreSchemaValidator}
import net.reactivecore.cjs.resolver.Downloader
import net.reactivecore.cjs.{DocumentValidator, Loader, Result}
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import java.security.*
import java.security.spec.*
import java.time.{Instant, ZonedDateTime}

@main def JwtCredentialValidationDemo(): Unit =

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

  val jwtCredentialPayload =
    JwtCredentialPayload(
      maybeIss = Some("https://example.edu/issuers/565049"), // ISSUER DID
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
      maybeNbf = Some(Instant.parse("2010-01-01T00:00:00Z")), // ISSUANCE DATE
      aud = Set.empty,
      maybeExp = Some(Instant.parse("2010-01-12T00:00:00Z")), // EXPIRATION DATE
      maybeJti = Some("http://example.edu/credentials/3732") // CREDENTIAL ID
    )

  def credentialSchemaResolver(credentialSchema: CredentialSchema): Json = {
    println("Resolving Schema")
    val resolvedSchema =
      """
        |{
        |  "type": "object",
        |  "properties": {
        |    "userName": {
        |      "$ref": "#/$defs/user"
        |    },
        |    "age": {
        |      "$ref": "#/$defs/age"
        |    },
        |    "email": {
        |      "$ref": "#/$defs/email"
        |    }
        |  },
        |  "required": ["userName", "age", "email"],
        |  "$defs": {
        |    "user": {
        |       "type": "string",
        |       "minLength": 3
        |     },
        |     "age": {
        |       "type": "number"
        |     },
        |     "email": {
        |       "type": "string",
        |       "format": "email"
        |     }
        |  }
        |}
        |""".stripMargin
    io.circe.parser.parse(resolvedSchema).toOption.get
  }

  println("")
  println("==================")
  println("Validate Good JWT Credential")
  println("==================")
  println(
    "Validated Payload" + jwtCredentialPayload.toValidatedCredentialPayload(credentialSchemaResolver)(
      PlaceholderSchemaValidator.fromSchema
    )
  )

  val emptyJwtCredentialPayload =
    JwtCredentialPayload(
      maybeIss = Some("https://example.edu/issuers/565049"), // ISSUER DID
      //      maybeIss = Option.empty, // ISSUER DID
      maybeSub = Some("1"), // SUBJECT DID
      //      maybeSub = Option.empty, // SUBJECT DID
      vc = JwtVc(
        `@context` = Set.empty,
        `type` = Set.empty,
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
      maybeNbf = Some(Instant.parse("2010-01-01T00:00:00Z")), // ISSUANCE DATE
      //      maybeNbf = Option.empty,
      aud = Set.empty,
      maybeExp = Some(Instant.parse("2010-01-12T00:00:00Z")), // EXPIRATION DATE
      maybeJti = Some("http://example.edu/credentials/3732") // CREDENTIAL ID
      //      maybeJti = Option.empty // CREDENTIAL ID
    )

  println("")
  println("==================")
  println("Validate Bad JWT Credential")
  println("==================")
  println(
    "Validated Payload" + emptyJwtCredentialPayload.toValidatedCredentialPayload(credentialSchemaResolver)(
      PlaceholderSchemaValidator.fromSchema
    )
  )
