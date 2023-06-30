package io.iohk.atala.pollux.vc.jwt

import com.nimbusds.jose.jwk.{Curve, ECKey}
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import io.circe.*
import io.circe.syntax.*
import io.iohk.atala.castor.core.model.did.VerificationRelationship
import io.iohk.atala.pollux.vc.jwt.CredentialPayload.Implicits.*
import zio.*
import zio.test.*
import zio.test.Assertion.*

import java.time.Instant

object JWTVerificationTest extends ZIOSpecDefault {

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
  ).when(!sys.props.get("os.name").contains("Mac OS X")) // Mac OS X throws `Curve not supported: secp256k1`

}
