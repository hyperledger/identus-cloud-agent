package org.hyperledger.identus.oid4vci.domain

import org.hyperledger.identus.agent.walletapi.memory.GenericSecretStorageInMemory
import org.hyperledger.identus.agent.walletapi.service.{ManagedDIDService, MockManagedDIDService}
import org.hyperledger.identus.agent.walletapi.storage.{DIDNonSecretStorage, MockDIDNonSecretStorage}
import org.hyperledger.identus.castor.core.model.did.VerificationRelationship
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
import org.hyperledger.identus.pollux.vc.jwt.PrismDidResolver
import org.hyperledger.identus.shared.models.WalletId
import zio.mock.MockSpecDefault
import zio.test.*
import zio.test.Assertion.*
import zio.{URLayer, ZIO, ZLayer}

object OIDCCredentialIssuerServiceSpec extends MockSpecDefault with CredentialServiceSpecHelper {

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
    OIDCCredentialIssuerServiceSpec
  )

  private val (issuerOp, issuerKp, issuerDidMetadata, issuerDidData) =
    MockDIDService.createDID(VerificationRelationship.AssertionMethod)

//  private val (holderOp, holderKp, holderDidMetadata, holderDidData) =
//    MockDIDService.createDID(VerificationRelationship.Authentication)
//
//  private val holderDidServiceExpectations =
//    MockDIDService.resolveDIDExpectation(holderDidMetadata, holderDidData)

  private val issuerDidServiceExpectations =
    MockDIDService.resolveDIDExpectation(issuerDidMetadata, issuerDidData)

  private val issuerManagedDIDServiceExpectations =
    MockManagedDIDService.javaKeyPairWithDIDExpectation(issuerKp)

  private val getIssuerPrismDidWalletIdExpectation =
    MockDIDNonSecretStorage.getPrismDidWalletIdExpectation(issuerDidData.id, WalletId.default)

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
