package io.iohk.atala.pollux.vc.jwt.demos

import com.nimbusds.jose.jwk.{Curve, ECKey}
import io.circe.*
import io.circe.syntax.*
import io.iohk.atala.pollux.vc.jwt.*
import io.iohk.atala.pollux.vc.jwt.CredentialPayload.Implicits.*
import io.iohk.atala.pollux.vc.jwt.PresentationPayload.Implicits.*
import pdi.jwt.JwtAlgorithm
import zio.Console.printLine
import zio.{UIO, ZIO, ZIOAppDefault}

import java.security.*
import java.security.interfaces.{ECPrivateKey, ECPublicKey}
import java.time.*
import scala.collection.immutable.Set

case class IssuerWithKey(issuer: Issuer, key: ECKey)
object JwtPresentationVerificationDemo extends ZIOAppDefault {
  def run =
    def createUser(did: DID): IssuerWithKey = {
      val keyGen = KeyPairGenerator.getInstance("EC")
      keyGen.initialize(Curve.P_256.toECParameterSpec)
      val keyPair = keyGen.generateKeyPair()
      val privateKey = keyPair.getPrivate
      val publicKey = keyPair.getPublic
      IssuerWithKey(
        Issuer(
          did = did,
          signer = ES256Signer(privateKey),
          publicKey = publicKey
        ),
        new ECKey.Builder(Curve.P_256, publicKey.asInstanceOf[ECPublicKey])
          .privateKey(privateKey.asInstanceOf[ECPrivateKey])
          .build()
      )
    }

    println("")
    println("==================")
    println("Create holder1")
    println("==================")
    val holder1 = createUser(DID("did:holder1:MDP8AsFhHzhwUvGNuYkX7T"))
    println(holder1)

    println("")
    println("==================")
    println("Create holder2")
    println("==================")
    val holder2 = createUser(DID("did:holder2:MDP8AsFhHzhwUvGNuYkX7T"))
    println(holder2)

    println("")
    println("==================")
    println("Create holder3")
    println("==================")
    val holder3 = createUser(DID("did:holder3:MDP8AsFhHzhwUvGNuYkX7T"))
    println(holder3)

    println("")
    println("==================")
    println("Create issuer1")
    println("==================")
    val issuer1 = createUser(DID("did:issuer1:MDP8AsFhHzhwUvGNuYkX7T"))
    println(issuer1)

    println("")
    println("==================")
    println("Create issuer1")
    println("==================")
    val issuer2 = createUser(DID("did:issuer2:MDP8AsFhHzhwUvGNuYkX7T"))
    println(issuer2)

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
        issuer = issuer1.issuer.did,
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

    val w3cIssuerSignedCredential = issuer1.issuer.signer.encode(w3cCredentialPayload.asJson)
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
        iss = issuer2.issuer.did.value, // ISSUER DID
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

    val jwtIssuerSignedCredential = issuer2.issuer.signer.encode(jwtCredentialPayload.asJson)
    val jwtVerifiableCredentialPayload = JwtVerifiableCredentialPayload(jwtIssuerSignedCredential)

    val jwtPresentationNbf = Instant.parse("2010-01-01T00:00:00Z") // ISSUANCE DATE
    val jwtPresentationExp = Instant.parse("2010-01-12T00:00:00Z") // EXPIRATION DATE

    def presentationPayload(
        maybeJwtPresentationNbf: Option[Instant],
        maybeJwtPresentationExp: Option[Instant]
    ): JwtPresentationPayload = {
      JwtPresentationPayload(
        iss = holder1.issuer.did.value,
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

    class DidResolverTest() extends DidResolver {

      private val resolverLookup: Map[String, DIDDocument] = Seq(holder1, holder2, holder3, issuer1, issuer2).map {
        issuerWithKey =>
          val did = issuerWithKey.issuer.did.value
          val verificationMethod =
            VerificationMethod(
              id = did,
              `type` = JwtAlgorithm.ES256.name,
              controller = "",
              publicKeyJwk = Some(
                toJWKFormat(issuerWithKey.key)
              )
            )
          did -> DIDDocument(
            id = did,
            alsoKnowAs = Vector.empty,
            controller = Vector.empty,
            verificationMethod = Vector(verificationMethod),
            service = Vector.empty
          )
      }.toMap

      override def resolve(didUrl: String): UIO[DIDResolutionResult] =
        resolverLookup
          .get(didUrl)
          .fold(
            ZIO.succeed(DIDResolutionFailed(NotFound(s"DIDDocument not found for $didUrl")))
          )((didDocument: DIDDocument) => {
            ZIO.succeed(
              DIDResolutionSucceeded(
                didDocument,
                DIDDocumentMetadata()
              )
            )
          })

    }

    println("")
    println("==================")
    println("Validate JWT Presentation Using DID Document of the issuer of the presentation")
    println("==================")
    val encodedJWTPresentation = JwtPresentation.encodeJwt(payload = jwtPresentationPayload, issuer = holder1.issuer)
    val encodedW3CPresentation =
      JwtPresentation.toEncodeW3C(payload = jwtPresentationPayload.toW3CPresentationPayload, issuer = holder1.issuer)
    val clock = Clock.system(ZoneId.systemDefault)
    for {
      _ <- printLine("DEMO TIME! ")
      w3cSignatureValidationResult <- JwtPresentation.validateEncodedW3C(encodedW3CPresentation.proof.jwt, None)(
        DidResolverTest()
      )
      jwtSignatureValidationResult <- JwtPresentation.validateEncodedJWT(encodedJWTPresentation, None)(
        DidResolverTest()
      )
      enclosedCredentialsValidationResult <-
        JwtPresentation.verify(
          encodedJWTPresentation,
          JwtPresentation.PresentationVerificationOptions(
            verifySignature = true,
            verifyDates = true,
            verifyHoldersBinding = true,
            leeway = Duration.ZERO,
            Some(CredentialVerification.CredentialVerificationOptions(verifySignature = true, verifyDates = true))
          )
        )(
          DidResolverTest()
        )(clock)
      _ <- printLine(s"W3C IS VALID?: $w3cSignatureValidationResult")
      _ <- printLine(s"JWT IS VALID?: $jwtSignatureValidationResult")
      _ <- printLine(s"Enclosed Credentials are VALID?: $enclosedCredentialsValidationResult")
    } yield ()
}
