package org.hyperledger.identus.pollux.vc.jwt

import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.jwk.{Curve, ECKey}
import io.circe.*
import io.circe.syntax.*
import org.hyperledger.identus.castor.core.model.did.VerificationRelationship
import org.hyperledger.identus.pollux.vc.jwt.CredentialPayload.Implicits.*
import zio.*
import zio.test.*
import zio.test.Assertion.*
import org.hyperledger.identus.shared.http.*
import java.security.Security
import java.time.Instant

object JWTVerificationTest extends ZIOSpecDefault {

  Security.insertProviderAt(BouncyCastleProviderSingleton.getInstance(), 2)

  case class IssuerWithKey(issuer: Issuer, key: ECKey)

  private def createUser(did: DID): IssuerWithKey = {
    val ecKey = ECKeyGenerator(Curve.SECP256K1).generate()
    IssuerWithKey(
      Issuer(
        did = did,
        signer = ES256KSigner(ecKey.toPrivateKey),
        publicKey = ecKey.toPublicKey
      ),
      ecKey
    )
  }

  private val statusListCredentialString = """
                                             |{
                                             |  "proof" : {
                                             |    "type" : "DataIntegrityProof",
                                             |    "proofPurpose" : "assertionMethod",
                                             |    "verificationMethod" : "data:application/json;base64,eyJAY29udGV4dCI6WyJodHRwczovL3czaWQub3JnL3NlY3VyaXR5L211bHRpa2V5L3YxIl0sInR5cGUiOiJNdWx0aWtleSIsInB1YmxpY0tleU11bHRpYmFzZSI6InVNRll3RUFZSEtvWkl6ajBDQVFZRks0RUVBQW9EUWdBRUNYSUZsMlIxOGFtZUxELXlrU09HS1FvQ0JWYkZNNW91bGtjMnZJckp0UzRQWkJnMkxyNEQzUFdYR2xHTXB1aHdwSk84MEFpdzFXeVVHT1hONkJqSlFBPT0ifQ==",
                                             |    "created" : "2024-03-04T14:44:43.867542Z",
                                             |    "proofValue" : "zAN1rKqPFt7JayDWWD4Gu7HRsNVrgqHxMhKmYT5AE1FYD5a2zaM8G4WRPBmss9M2h3J5f56sunDFbxJVuDGB8qndknijyBcqr3",
                                             |    "cryptoSuite" : "eddsa-jcs-2022"
                                             |  },
                                             |  "@context" : [
                                             |    "https://www.w3.org/2018/credentials/v1",
                                             |    "https://w3id.org/vc/status-list/2021/v1"
                                             |  ],
                                             |  "type" : [
                                             |    "VerifiableCredential",
                                             |    "StatusList2021Credential"
                                             |  ],
                                             |  "id" : "http://localhost:8085/credential-status/664382dc-9e6d-4d0c-99d1-85e2c74eb5e9",
                                             |  "issuer" : "did:prism:462c4811bf61d7de25b3baf86c5d2f0609b4debe53792d297bf612269bf8593a",
                                             |  "issuanceDate" : 1709563483,
                                             |  "credentialSubject" : {
                                             |    "id" : "",
                                             |    "type" : "StatusList2021",
                                             |    "statusPurpose" : "Revocation",
                                             |    "encodedList" : "H4sIAAAAAAAA_-3BMQ0AAAACIGf_0MbwARoAAAAAAAAAAAAAAAAAAADgbbmHB0sAQAAA"
                                             |  }
                                             |}
                                             |""".stripMargin

  private def createJwtCredential(issuer: IssuerWithKey): JWT = {
    val jwtCredentialNbf = Instant.parse("2010-01-01T00:00:00Z") // ISSUANCE DATE
    val jwtCredentialExp = Instant.parse("2010-01-12T00:00:00Z") // EXPIRATION DATE
    val jwtCredentialPayload = JwtCredentialPayload(
      iss = issuer.issuer.did.value,
      maybeSub = Some("1"),
      vc = JwtVc(
        `@context` = Set("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
        `type` = Set("VerifiableCredential", "UniversityDegreeCredential"),
        maybeCredentialSchema = None,
        credentialSubject = Json.obj("id" -> Json.fromString("1")),
        maybeCredentialStatus = None,
        maybeRefreshService = None,
        maybeEvidence = None,
        maybeTermsOfUse = None
      ),
      nbf = jwtCredentialNbf, // ISSUANCE DATE
      aud = Set.empty,
      maybeExp = Some(jwtCredentialExp), // EXPIRATION DATE
      maybeJti = Some("http://example.edu/credentials/3732") // CREDENTIAL ID
    )
    issuer.issuer.signer.encode(jwtCredentialPayload.asJson)
  }

  private def generateDidDocument(
      did: String,
      verificationMethod: Vector[VerificationMethod] = Vector.empty,
      authentication: Vector[VerificationMethodOrRef] = Vector.empty,
      assertionMethod: Vector[VerificationMethodOrRef] = Vector.empty,
      keyAgreement: Vector[VerificationMethodOrRef] = Vector.empty,
      capabilityInvocation: Vector[VerificationMethodOrRef] = Vector.empty,
      capabilityDelegation: Vector[VerificationMethodOrRef] = Vector.empty,
      service: Vector[Service] = Vector.empty
  ): DIDDocument =
    DIDDocument(
      id = did,
      alsoKnowAs = Vector.empty,
      controller = Vector.empty,
      verificationMethod = verificationMethod,
      authentication = authentication,
      assertionMethod = assertionMethod,
      keyAgreement = keyAgreement,
      capabilityInvocation = capabilityInvocation,
      capabilityDelegation = capabilityDelegation,
      service = service
    )

  private def makeResolver(lookup: Map[String, DIDDocument]): DidResolver = (didUrl: String) => {
    lookup
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

  override def spec = suite("JWTVerificationSpec")(
    test("validate status list credential proof and revocation status by index") {
      val statusList: CredentialStatus = CredentialStatus(
        id = "http://localhost:8085/credential-status/664382dc-9e6d-4d0c-99d1-85e2c74eb5e9#1",
        statusPurpose = StatusPurpose.Revocation,
        `type` = "StatusList2021Entry",
        statusListCredential = "http://localhost:8085/credential-status/664382dc-9e6d-4d0c-99d1-85e2c74eb5e9",
        statusListIndex = 2
      )

      val urlResolver = new UriResolver {
        override def resolve(uri: String): IO[GenericUriResolverError, String] = {
          ZIO.succeed(statusListCredentialString)
        }
      }

      val genericUriResolver = GenericUriResolver(
        Map(
          "data" -> DataUrlResolver(),
          "http" -> urlResolver,
          "https" -> urlResolver
        )
      )
      for {
        validation <- CredentialVerification.verifyCredentialStatus(statusList)(genericUriResolver)
      } yield assertTrue(validation.fold(_ => false, _ => true))
    },
    test("fail verification if proof is valid but credential is revoked at the give status list index") {
      val statusList: CredentialStatus = CredentialStatus(
        id = "http://localhost:8085/credential-status/664382dc-9e6d-4d0c-99d1-85e2c74eb5e9#1",
        statusPurpose = StatusPurpose.Revocation,
        `type` = "StatusList2021Entry",
        statusListCredential = "http://localhost:8085/credential-status/664382dc-9e6d-4d0c-99d1-85e2c74eb5e9",
        statusListIndex = 1
      )

      val urlResolver = new UriResolver {
        override def resolve(uri: String): IO[GenericUriResolverError, String] = {
          ZIO.succeed(statusListCredentialString)
        }
      }

      val genericUriResolver = GenericUriResolver(
        Map(
          "data" -> DataUrlResolver(),
          "http" -> urlResolver,
          "https" -> urlResolver
        )
      )
      for {
        validation <- CredentialVerification.verifyCredentialStatus(statusList)(genericUriResolver)
      } yield assertTrue(
        validation.fold(
          chunk => chunk.length == 1 && chunk.head.contentEquals("Credential is revoked"),
          _ => false
        )
      )
    },
    test("validate PrismDID issued JWT VC using verification publicKeys") {
      val issuer = createUser(DID("did:prism:issuer"))
      val jwtCredential = createJwtCredential(issuer)
      val resolver = makeResolver(
        Map(
          "did:prism:issuer" ->
            generateDidDocument(
              did = "did:prism:issuer",
              verificationMethod = Vector(
                VerificationMethod(
                  id = "did:prism:issuer#key0",
                  `type` = "EcdsaSecp256k1VerificationKey2019",
                  controller = "did:prism:issuer",
                  publicKeyJwk = Some(toJWKFormat(issuer.key))
                )
              )
            )
        )
      )
      for {
        validation <- JwtCredential.validateEncodedJWT(jwtCredential)(resolver)
      } yield assertTrue(validation.fold(_ => false, _ => true))
    },
    test("validate PrismDID issued JWT VC using specified proofPurpose resolved as embedded key") {
      val issuer = createUser(DID("did:prism:issuer"))
      val jwtCredential = createJwtCredential(issuer)
      val resolver = makeResolver(
        Map(
          "did:prism:issuer" ->
            generateDidDocument(
              did = "did:prism:issuer",
              assertionMethod = Vector(
                VerificationMethod(
                  id = "did:prism:issuer#key0",
                  `type` = "EcdsaSecp256k1VerificationKey2019",
                  controller = "did:prism:issuer",
                  publicKeyJwk = Some(toJWKFormat(issuer.key))
                )
              )
            )
        )
      )
      for {
        validation <- JwtCredential.validateEncodedJWT(jwtCredential, Some(VerificationRelationship.AssertionMethod))(
          resolver
        )
      } yield assertTrue(validation.fold(_ => false, _ => true))
    },
    test("validate PrismDID issued JWT VC using specified proofPurpose resolved as referenced key") {
      val issuer = createUser(DID("did:prism:issuer"))
      val jwtCredential = createJwtCredential(issuer)
      val resolver = makeResolver(
        Map(
          "did:prism:issuer" ->
            generateDidDocument(
              did = "did:prism:issuer",
              verificationMethod = Vector(
                VerificationMethod(
                  id = "did:prism:issuer#key0",
                  `type` = "EcdsaSecp256k1VerificationKey2019",
                  controller = "did:prism:issuer",
                  publicKeyJwk = Some(toJWKFormat(issuer.key))
                )
              ),
              assertionMethod = Vector("did:prism:issuer#key0")
            )
        )
      )
      for {
        validation <- JwtCredential.validateEncodedJWT(jwtCredential, Some(VerificationRelationship.AssertionMethod))(
          resolver
        )
      } yield assertTrue(validation.fold(_ => false, _ => true))
    },
    test("validate PrismDID issued JWT VC using incorrect proofPurpose should fail") {
      val issuer = createUser(DID("did:prism:issuer"))
      val jwtCredential = createJwtCredential(issuer)
      val resolver = makeResolver(
        Map(
          "did:prism:issuer" ->
            generateDidDocument(
              did = "did:prism:issuer",
              authentication = Vector(
                VerificationMethod(
                  id = "did:prism:issuer#key0",
                  `type` = "EcdsaSecp256k1VerificationKey2019",
                  controller = "did:prism:issuer",
                  publicKeyJwk = Some(toJWKFormat(issuer.key))
                )
              )
            )
        )
      )
      for {
        validation <- JwtCredential.validateEncodedJWT(jwtCredential, Some(VerificationRelationship.AssertionMethod))(
          resolver
        )
      } yield assert(validation.fold(_ => false, _ => true))(equalTo(false))
    },
    test("validate PrismDID issued JWT VC using non-resolvable DID should fail") {
      val issuer = createUser(DID("did:prism:issuer"))
      val jwtCredential = createJwtCredential(issuer)
      val resolver = makeResolver(Map.empty)
      for {
        validation <- JwtCredential.validateEncodedJWT(jwtCredential)(resolver)
      } yield assert(validation.fold(_ => false, _ => true))(equalTo(false))
    },
    test("validate PrismDID issued JWT VC using non-existing public-key should fail") {
      val issuer = createUser(DID("did:prism:issuer"))
      val jwtCredential = createJwtCredential(issuer)
      val resolver = makeResolver(Map("did:prism:issuer" -> generateDidDocument(did = "did:prism:issuer")))
      for {
        validation <- JwtCredential.validateEncodedJWT(jwtCredential)(resolver)
      } yield assert(validation.fold(_ => false, _ => true))(equalTo(false))
    },
    test("validate PrismDID issued JWT VC using incompatible public-key type should fail") {
      val issuer = createUser(DID("did:prism:issuer"))
      val jwtCredential = createJwtCredential(issuer)
      val resolver = makeResolver(
        Map(
          "did:prism:issuer" ->
            generateDidDocument(
              did = "did:prism:issuer",
              verificationMethod = Vector(
                VerificationMethod(
                  id = "did:prism:issuer#key0",
                  `type` = "ThisIsInvalidPublicKeyType",
                  controller = "did:prism:issuer",
                  publicKeyJwk = Some(toJWKFormat(issuer.key))
                )
              )
            )
        )
      )
      for {
        validation <- JwtCredential.validateEncodedJWT(jwtCredential)(resolver)
      } yield assert(validation.fold(_ => false, _ => true))(equalTo(false))
    },
    test("validate PrismDID issued JWT VC using different ECKey should fail") {
      val issuer = createUser(DID("did:prism:issuer"))
      val jwtCredential = createJwtCredential(issuer)
      val resolver = makeResolver(
        Map(
          "did:prism:issuer" ->
            generateDidDocument(
              did = "did:prism:issuer",
              verificationMethod = Vector(
                VerificationMethod(
                  id = "did:prism:issuer#key0",
                  `type` = "EcdsaSecp256k1VerificationKey2019",
                  controller = "did:prism:issuer",
                  publicKeyJwk = Some(toJWKFormat(ECKeyGenerator(Curve.SECP256K1).generate()))
                )
              )
            )
        )
      )
      for {
        validation <- JwtCredential.validateEncodedJWT(jwtCredential)(resolver)
      } yield assert(validation.fold(_ => false, _ => true))(equalTo(false))
    }
  )

}
