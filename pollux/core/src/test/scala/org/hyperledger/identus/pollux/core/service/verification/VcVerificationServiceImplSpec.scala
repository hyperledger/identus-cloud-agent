package org.hyperledger.identus.pollux.core.service.verification

import io.circe.*
import io.circe.syntax.*
import org.hyperledger.identus.agent.walletapi.service.MockManagedDIDService
import org.hyperledger.identus.castor.core.service.MockDIDService
import org.hyperledger.identus.pollux.core.service.ResourceURIDereferencerImpl
import org.hyperledger.identus.pollux.vc.jwt.*
import org.hyperledger.identus.pollux.vc.jwt.CredentialPayload.Implicits.*
import org.hyperledger.identus.shared.models.{WalletAccessContext, WalletId}
import zio.*
import zio.Config.OffsetDateTime
import zio.test.*

import java.time.Instant

object VcVerificationServiceImplSpec extends ZIOSpecDefault with VcVerificationServiceSpecHelper {

  override def spec = {
    suite("VcVerificationServiceImpl")(
      test("verify aud given valid") {
        for {
          svc <- ZIO.service[VcVerificationService]
          verifier = DID("did:prism:verifier")
          jwtCredentialPayload = W3cCredentialPayload(
            `@context` =
              Set("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
            maybeId = Some("http://example.edu/credentials/3732"),
            `type` = Set("VerifiableCredential", "UniversityDegreeCredential"),
            issuer = issuer.did,
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
                `type` = "StatusList2021Entry",
                statusPurpose = StatusPurpose.Revocation,
                statusListIndex = 0,
                statusListCredential = "https://example.com/credentials/status/3"
              )
            ),
            maybeRefreshService = Some(
              RefreshService(
                id = "https://example.edu/refresh/3732",
                `type` = "ManualRefreshService2018"
              )
            ),
            maybeEvidence = Option.empty,
            maybeTermsOfUse = Option.empty,
            aud = Set(verifier.value)
          ).toJwtCredentialPayload
          signedJwtCredential = issuer.signer.encode(jwtCredentialPayload.asJson)
          result <-
            svc.verify(
              List(
                VcVerificationRequest(signedJwtCredential.value, VcVerification.AudienceCheck(verifier.value))
              )
            )
        } yield {
          assertTrue(
            result.contains(
              VcVerificationResult(signedJwtCredential.value, VcVerification.AudienceCheck(verifier.value), true)
            )
          )
        }
      }.provideSomeLayer(
        MockDIDService.empty ++
          MockManagedDIDService.empty ++
          ResourceURIDereferencerImpl.layer >+>
          someVcVerificationServiceLayer ++
          ZLayer.succeed(WalletAccessContext(WalletId.random))
      ),
      test("verify aud given invalid") {
        for {
          svc <- ZIO.service[VcVerificationService]
          issuerDid = DID(issuerDidData.id.toString)
          verifier = DID("did:prism:verifier")
          jwtCredentialPayload = W3cCredentialPayload(
            `@context` =
              Set("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
            maybeId = Some("http://example.edu/credentials/3732"),
            `type` = Set("VerifiableCredential", "UniversityDegreeCredential"),
            issuer = issuer.did,
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
                `type` = "StatusList2021Entry",
                statusPurpose = StatusPurpose.Revocation,
                statusListIndex = 0,
                statusListCredential = "https://example.com/credentials/status/3"
              )
            ),
            maybeRefreshService = Some(
              RefreshService(
                id = "https://example.edu/refresh/3732",
                `type` = "ManualRefreshService2018"
              )
            ),
            maybeEvidence = Option.empty,
            maybeTermsOfUse = Option.empty,
            aud = Set(verifier.value)
          ).toJwtCredentialPayload
          signedJwtCredential = issuer.signer.encode(jwtCredentialPayload.asJson)
          result <-
            svc.verify(
              List(
                VcVerificationRequest(signedJwtCredential.value, VcVerification.AudienceCheck(issuer.did.value))
              )
            )
        } yield {
          assertTrue(
            result.contains(
              VcVerificationResult(signedJwtCredential.value, VcVerification.AudienceCheck(issuer.did.value), false)
            )
          )
        }
      }.provideSomeLayer(
        MockDIDService.empty ++
          MockManagedDIDService.empty ++
          ResourceURIDereferencerImpl.layer >+>
          someVcVerificationServiceLayer ++
          ZLayer.succeed(WalletAccessContext(WalletId.random))
      ),
      test("verify signature given valid") {
        for {
          svc <- ZIO.service[VcVerificationService]
          verifier = DID("did:prism:verifier")
          jwtCredentialPayload = W3cCredentialPayload(
            `@context` =
              Set("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
            maybeId = Some("http://example.edu/credentials/3732"),
            `type` = Set("VerifiableCredential", "UniversityDegreeCredential"),
            issuer = issuer.did,
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
                `type` = "StatusList2021Entry",
                statusPurpose = StatusPurpose.Revocation,
                statusListIndex = 0,
                statusListCredential = "https://example.com/credentials/status/3"
              )
            ),
            maybeRefreshService = Some(
              RefreshService(
                id = "https://example.edu/refresh/3732",
                `type` = "ManualRefreshService2018"
              )
            ),
            maybeEvidence = Option.empty,
            maybeTermsOfUse = Option.empty,
            aud = Set(verifier.value)
          ).toJwtCredentialPayload
          signedJwtCredential = issuer.signer.encode(jwtCredentialPayload.asJson)
          result <-
            svc.verify(
              List(
                VcVerificationRequest(signedJwtCredential.value, VcVerification.SignatureVerification)
              )
            )
        } yield {
          assertTrue(
            result.contains(
              VcVerificationResult(signedJwtCredential.value, VcVerification.SignatureVerification, true)
            )
          )
        }
      }.provideSomeLayer(
        issuerDidServiceExpectations.toLayer ++
          MockManagedDIDService.empty ++
          ResourceURIDereferencerImpl.layer >+>
          someVcVerificationServiceLayer ++
          ZLayer.succeed(WalletAccessContext(WalletId.random))
      ),
      test("verify issuer given valid") {
        for {
          svc <- ZIO.service[VcVerificationService]
          verifier = DID("did:prism:verifier")
          jwtCredentialPayload = W3cCredentialPayload(
            `@context` =
              Set("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
            maybeId = Some("http://example.edu/credentials/3732"),
            `type` = Set("VerifiableCredential", "UniversityDegreeCredential"),
            issuer = issuer.did,
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
                `type` = "StatusList2021Entry",
                statusPurpose = StatusPurpose.Revocation,
                statusListIndex = 0,
                statusListCredential = "https://example.com/credentials/status/3"
              )
            ),
            maybeRefreshService = Some(
              RefreshService(
                id = "https://example.edu/refresh/3732",
                `type` = "ManualRefreshService2018"
              )
            ),
            maybeEvidence = Option.empty,
            maybeTermsOfUse = Option.empty,
            aud = Set(verifier.value)
          ).toJwtCredentialPayload
          signedJwtCredential = issuer.signer.encode(jwtCredentialPayload.asJson)
          result <-
            svc.verify(
              List(
                VcVerificationRequest(signedJwtCredential.value, VcVerification.IssuerIdentification(issuer.did.value))
              )
            )
        } yield {
          assertTrue(
            result.contains(
              VcVerificationResult(
                signedJwtCredential.value,
                VcVerification.IssuerIdentification(issuer.did.value),
                true
              )
            )
          )
        }
      }.provideSomeLayer(
        MockDIDService.empty ++
          MockManagedDIDService.empty ++
          ResourceURIDereferencerImpl.layer >+>
          someVcVerificationServiceLayer ++
          ZLayer.succeed(WalletAccessContext(WalletId.random))
      ),
      test("verify issuer given invalid") {
        for {
          svc <- ZIO.service[VcVerificationService]
          issuerDid = DID(issuerDidData.id.toString)
          verifier = DID("did:prism:verifier")
          jwtCredentialPayload = W3cCredentialPayload(
            `@context` =
              Set("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
            maybeId = Some("http://example.edu/credentials/3732"),
            `type` = Set("VerifiableCredential", "UniversityDegreeCredential"),
            issuer = issuer.did,
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
                `type` = "StatusList2021Entry",
                statusPurpose = StatusPurpose.Revocation,
                statusListIndex = 0,
                statusListCredential = "https://example.com/credentials/status/3"
              )
            ),
            maybeRefreshService = Some(
              RefreshService(
                id = "https://example.edu/refresh/3732",
                `type` = "ManualRefreshService2018"
              )
            ),
            maybeEvidence = Option.empty,
            maybeTermsOfUse = Option.empty,
            aud = Set(verifier.value)
          ).toJwtCredentialPayload
          signedJwtCredential = issuer.signer.encode(jwtCredentialPayload.asJson)
          result <-
            svc.verify(
              List(
                VcVerificationRequest(signedJwtCredential.value, VcVerification.IssuerIdentification(verifier.value))
              )
            )
        } yield {
          assertTrue(
            result.contains(
              VcVerificationResult(
                signedJwtCredential.value,
                VcVerification.IssuerIdentification(verifier.value),
                false
              )
            )
          )
        }
      }.provideSomeLayer(
        MockDIDService.empty ++
          MockManagedDIDService.empty ++
          ResourceURIDereferencerImpl.layer >+>
          someVcVerificationServiceLayer ++
          ZLayer.succeed(WalletAccessContext(WalletId.random))
      ),
      test("verify nbf given valid") {
        for {
          svc <- ZIO.service[VcVerificationService]
          verifier = DID("did:prism:verifier")
          currentTime = OffsetDateTime.parse("2010-01-01T00:00:00Z").toOption.get
          jwtCredentialPayload = W3cCredentialPayload(
            `@context` =
              Set("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
            maybeId = Some("http://example.edu/credentials/3732"),
            `type` = Set("VerifiableCredential", "UniversityDegreeCredential"),
            issuer = issuer.did,
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
                `type` = "StatusList2021Entry",
                statusPurpose = StatusPurpose.Revocation,
                statusListIndex = 0,
                statusListCredential = "https://example.com/credentials/status/3"
              )
            ),
            maybeRefreshService = Some(
              RefreshService(
                id = "https://example.edu/refresh/3732",
                `type` = "ManualRefreshService2018"
              )
            ),
            maybeEvidence = Option.empty,
            maybeTermsOfUse = Option.empty,
            aud = Set(verifier.value)
          ).toJwtCredentialPayload
          signedJwtCredential = issuer.signer.encode(jwtCredentialPayload.asJson)
          result <-
            svc.verify(
              List(
                VcVerificationRequest(signedJwtCredential.value, VcVerification.NotBeforeCheck(currentTime))
              )
            )
        } yield {
          assertTrue(
            result.contains(
              VcVerificationResult(signedJwtCredential.value, VcVerification.NotBeforeCheck(currentTime), true)
            )
          )
        }
      }.provideSomeLayer(
        MockDIDService.empty ++
          MockManagedDIDService.empty ++
          ResourceURIDereferencerImpl.layer >+>
          someVcVerificationServiceLayer ++
          ZLayer.succeed(WalletAccessContext(WalletId.random))
      ),
      test("verify nbf given invalid") {
        for {
          svc <- ZIO.service[VcVerificationService]
          verifier = DID("did:prism:verifier")
          currentTime = OffsetDateTime.parse("2010-01-01T00:00:00Z").toOption.get.minusDays(2)
          jwtCredentialPayload = W3cCredentialPayload(
            `@context` =
              Set("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
            maybeId = Some("http://example.edu/credentials/3732"),
            `type` = Set("VerifiableCredential", "UniversityDegreeCredential"),
            issuer = issuer.did,
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
                `type` = "StatusList2021Entry",
                statusPurpose = StatusPurpose.Revocation,
                statusListIndex = 0,
                statusListCredential = "https://example.com/credentials/status/3"
              )
            ),
            maybeRefreshService = Some(
              RefreshService(
                id = "https://example.edu/refresh/3732",
                `type` = "ManualRefreshService2018"
              )
            ),
            maybeEvidence = Option.empty,
            maybeTermsOfUse = Option.empty,
            aud = Set(verifier.value)
          ).toJwtCredentialPayload
          signedJwtCredential = issuer.signer.encode(jwtCredentialPayload.asJson)
          result <-
            svc.verify(
              List(
                VcVerificationRequest(signedJwtCredential.value, VcVerification.NotBeforeCheck(currentTime))
              )
            )
        } yield {
          assertTrue(
            result.contains(
              VcVerificationResult(signedJwtCredential.value, VcVerification.NotBeforeCheck(currentTime), false)
            )
          )
        }
      }.provideSomeLayer(
        MockDIDService.empty ++
          MockManagedDIDService.empty ++
          ResourceURIDereferencerImpl.layer >+>
          someVcVerificationServiceLayer ++
          ZLayer.succeed(WalletAccessContext(WalletId.random))
      ),
      test("verify exp given valid") {
        for {
          svc <- ZIO.service[VcVerificationService]
          verifier = DID("did:prism:verifier")
          currentTime = OffsetDateTime.parse("2010-01-01T00:00:00Z").toOption.get
          jwtCredentialPayload = W3cCredentialPayload(
            `@context` =
              Set("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
            maybeId = Some("http://example.edu/credentials/3732"),
            `type` = Set("VerifiableCredential", "UniversityDegreeCredential"),
            issuer = issuer.did,
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
                `type` = "StatusList2021Entry",
                statusPurpose = StatusPurpose.Revocation,
                statusListIndex = 0,
                statusListCredential = "https://example.com/credentials/status/3"
              )
            ),
            maybeRefreshService = Some(
              RefreshService(
                id = "https://example.edu/refresh/3732",
                `type` = "ManualRefreshService2018"
              )
            ),
            maybeEvidence = Option.empty,
            maybeTermsOfUse = Option.empty,
            aud = Set(verifier.value)
          ).toJwtCredentialPayload
          signedJwtCredential = issuer.signer.encode(jwtCredentialPayload.asJson)
          result <-
            svc.verify(
              List(
                VcVerificationRequest(signedJwtCredential.value, VcVerification.ExpirationCheck(currentTime))
              )
            )
        } yield {
          assertTrue(
            result.contains(
              VcVerificationResult(signedJwtCredential.value, VcVerification.ExpirationCheck(currentTime), true)
            )
          )
        }
      }.provideSomeLayer(
        MockDIDService.empty ++
          MockManagedDIDService.empty ++
          ResourceURIDereferencerImpl.layer >+>
          someVcVerificationServiceLayer ++
          ZLayer.succeed(WalletAccessContext(WalletId.random))
      ),
      test("verify exp given invalid") {
        for {
          svc <- ZIO.service[VcVerificationService]
          verifier = DID("did:prism:verifier")
          currentTime = OffsetDateTime.parse("2010-01-12T00:00:00Z").toOption.get
          jwtCredentialPayload = W3cCredentialPayload(
            `@context` =
              Set("https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"),
            maybeId = Some("http://example.edu/credentials/3732"),
            `type` = Set("VerifiableCredential", "UniversityDegreeCredential"),
            issuer = issuer.did,
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
                `type` = "StatusList2021Entry",
                statusPurpose = StatusPurpose.Revocation,
                statusListIndex = 0,
                statusListCredential = "https://example.com/credentials/status/3"
              )
            ),
            maybeRefreshService = Some(
              RefreshService(
                id = "https://example.edu/refresh/3732",
                `type` = "ManualRefreshService2018"
              )
            ),
            maybeEvidence = Option.empty,
            maybeTermsOfUse = Option.empty,
            aud = Set(verifier.value)
          ).toJwtCredentialPayload
          signedJwtCredential = issuer.signer.encode(jwtCredentialPayload.asJson)
          result <-
            svc.verify(
              List(
                VcVerificationRequest(signedJwtCredential.value, VcVerification.ExpirationCheck(currentTime))
              )
            )
        } yield {
          assertTrue(
            result.contains(
              VcVerificationResult(signedJwtCredential.value, VcVerification.ExpirationCheck(currentTime), false)
            )
          )
        }
      }.provideSomeLayer(
        MockDIDService.empty ++
          MockManagedDIDService.empty ++
          ResourceURIDereferencerImpl.layer >+>
          someVcVerificationServiceLayer ++
          ZLayer.succeed(WalletAccessContext(WalletId.random))
      )
    )
  }
}
