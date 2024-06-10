package org.hyperledger.identus.oid4vci.domain

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.*
import com.nimbusds.jose.jwk.*
import com.nimbusds.jose.jwk.gen.*
import org.hyperledger.identus.agent.walletapi.memory.GenericSecretStorageInMemory
import org.hyperledger.identus.agent.walletapi.service.{ManagedDIDService, MockManagedDIDService}
import org.hyperledger.identus.agent.walletapi.storage.{DIDNonSecretStorage, MockDIDNonSecretStorage}
import org.hyperledger.identus.castor.core.model.did.{PrismDID, VerificationRelationship}
import org.hyperledger.identus.castor.core.service.{DIDService, MockDIDService}
import org.hyperledger.identus.oid4vci.http.{ClaimDescriptor, CredentialDefinition, Localization}
import org.hyperledger.identus.oid4vci.service.{OIDCCredentialIssuerService, OIDCCredentialIssuerServiceImpl}
import org.hyperledger.identus.oid4vci.storage.InMemoryIssuanceSessionService
import org.hyperledger.identus.pollux.core.repository.{
  CredentialRepository,
  CredentialRepositoryInMemory,
  CredentialStatusListRepositoryInMemory
}
import org.hyperledger.identus.pollux.core.service.*
import org.hyperledger.identus.pollux.vc.jwt.{ES256KSigner, PrismDidResolver}
import org.hyperledger.identus.pollux.vc.jwt.JWT
import org.hyperledger.identus.shared.models.WalletId
import zio.{URLayer, ZIO, ZLayer}
import zio.mock.MockSpecDefault
import zio.test.*
import zio.test.Assertion.*
import zio.Clock
import zio.Random

import java.util.UUID
import scala.jdk.CollectionConverters._

object OIDCCredentialIssuerServiceSpec
    extends MockSpecDefault
    with CredentialServiceSpecHelper
    with Openid4VCIProofJwtOps {

  val layers: URLayer[
    DIDService & ManagedDIDService & DIDNonSecretStorage,
    CredentialService & CredentialDefinitionService & OIDCCredentialIssuerService
  ] =
    ZLayer.makeSome[
      DIDService & ManagedDIDService & DIDNonSecretStorage,
      CredentialService & CredentialDefinitionService & OIDCCredentialIssuerService
    ](
      InMemoryIssuanceSessionService.layer,
      CredentialRepositoryInMemory.layer,
      CredentialStatusListRepositoryInMemory.layer,
      PrismDidResolver.layer,
      ResourceURIDereferencerImpl.layer,
      credentialDefinitionServiceLayer,
      GenericSecretStorageInMemory.layer,
      LinkSecretServiceImpl.layer,
      CredentialServiceImpl.layer,
      OIDCCredentialIssuerServiceImpl.layer
    )

  override def spec = suite("CredentialServiceImpl")(
    OIDCCredentialIssuerServiceSpec,
    validateProofSpec
  )

  private val (issuerOp, issuerKp, issuerDidMetadata, issuerDidData) =
    MockDIDService.createDID(VerificationRelationship.AssertionMethod)

  private val (holderOp, holderKp, holderDidMetadata, holderDidData) =
    MockDIDService.createDID(VerificationRelationship.AssertionMethod)

  private val holderDidServiceExpectations =
    MockDIDService.resolveDIDExpectation(holderDidMetadata, holderDidData)

  private val holderManagedDIDServiceExpectations =
    MockManagedDIDService.javaKeyPairWithDIDExpectation(holderKp)

  private val issuerDidServiceExpectations =
    MockDIDService.resolveDIDExpectation(issuerDidMetadata, issuerDidData)

  private val issuerManagedDIDServiceExpectations =
    MockManagedDIDService.javaKeyPairWithDIDExpectation(issuerKp)

  private val getIssuerPrismDidWalletIdExpectation =
    MockDIDNonSecretStorage.getPrismDidWalletIdExpectation(issuerDidData.id, WalletId.default)

  private def buildJwtProof(nonce: String, aud: UUID, iat: Int) = {
    import org.bouncycastle.util.encoders.Hex

    val longFormDid = PrismDID.buildLongFormFromOperation(holderOp)

    val encodedKey = Hex.toHexString(holderKp.privateKey.getEncoded)
    println(s"Private Key: $encodedKey")
    println("Long Form DID: " + longFormDid.toString)

    makeJwtProof(longFormDid, nonce, aud, iat, holderKp.privateKey)
  }

  private val validateProofSpec = suite("Validate holder's proof of possession using the LongFormPrismDID")(
    test("should validate the holder's proof of possession using the LongFormPrismDID") {
      for {
        credentialIssuer <- ZIO.service[OIDCCredentialIssuerService]
        nonce <- Random.nextString(10)
        aud <- Random.nextUUID
        iat <- Clock.instant.map(_.getEpochSecond.toInt)
        jwt = buildJwtProof(nonce, aud, iat)
        result <- credentialIssuer.verifyJwtProof(jwt)
      } yield assert(result)(equalTo(true))
    }.provideSomeLayer(
      holderDidServiceExpectations.toLayer ++
        MockManagedDIDService.empty ++
        // holderManagedDIDServiceExpectations.toLayer ++
        MockDIDNonSecretStorage.empty >+> layers
    )
  )

  private val OIDCCredentialIssuerServiceSpec =
    suite("Simple JWT credential issuance")(
      test("should issue a JWT credential") {
        for {
          oidcCredentialIssuerService <- ZIO.service[OIDCCredentialIssuerService]
          credentialDefinition = CredentialDefinition(
            `@context` = Some(Seq("https://www.w3.org/2018/credentials/v1")),
            `type` = Seq("VerifiableCredential", "CertificateOfBirth"),
            credentialSubject = Some(
              Map(
                "name" ->
                  ClaimDescriptor(mandatory = Some(true), valueType = Some("string"), display = Seq.empty[Localization])
              )
            )
          )
          jwt <- oidcCredentialIssuerService.issueJwtCredential(issuerDidData.id, None, credentialDefinition)
          _ <- zio.Console.printLine(jwt)
        } yield assert(jwt.toString)(isNonEmptyString)
      }.provideSomeLayer(
        issuerDidServiceExpectations.toLayer ++
          issuerManagedDIDServiceExpectations.toLayer ++
          getIssuerPrismDidWalletIdExpectation.toLayer >+> layers
      )
    )
}
