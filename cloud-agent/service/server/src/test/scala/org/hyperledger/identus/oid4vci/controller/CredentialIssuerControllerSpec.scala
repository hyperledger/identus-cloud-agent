package org.hyperledger.identus.oid4vci.controller

import org.hyperledger.identus.oid4vci.domain.Openid4VCIProofJwtOps
import org.hyperledger.identus.pollux.core.service.CredentialServiceSpecHelper
import zio.test.{assertTrue, ZIOSpecDefault}
import zio.test.TestAspect.sequential

object CredentialIssuerControllerSpec
    extends ZIOSpecDefault
    with CredentialServiceSpecHelper
    with Openid4VCIProofJwtOps {

  override def spec = suite("CredentialIssuerController")(authorizationCodeFlowSpec1a, preAutorizedCodeFlowSpec)

  val authorizationCodeFlowSpec1a = suite("Authorization Code Flow 1a")(
    test(
      "1a: The Wallet-initiated flow begins as the End-User requests a Credential via the Wallet from the Credential Issuer."
    ) {
      assertTrue(true)
    },
    test("2: The Wallet uses the Credential Issuer's URL to fetch the Credential Issuer metadata") {
      assertTrue(true)
    },
    test("3: The Wallet sends an Authorization Request to the Authorization Endpoint") {
      assertTrue(true)
    },
    test(
      "4: The Authorization Endpoint returns the Authorization Response with the Authorization Code upon successfully processing the Authorization Request"
    ) {
      assertTrue(true)
    },
    test("5: The Wallet sends a Token Request to the Token Endpoint with the Authorization Code") {
      assertTrue(true)
    },
    test(
      "6: The Wallet sends a Credential Request to the Credential Issuer's Credential Endpoint with the Access Token"
    ) {
      assertTrue(true)
    },
  ) @@ sequential

  val autorizationCodeFlowSpec1b = suite("Authorization Code Flow 1b")(
    test("The Issuer-initiated flow begins as the Credential Issuer generates a Credential Offer") {
      assertTrue(true)
    },
    test("2: The Wallet uses the Credential Issuer's URL to fetch the Credential Issuer metadata") {
      assertTrue(true)
    },
    test("3: The Wallet sends an Authorization Request to the Authorization Endpoint") {
      assertTrue(true)
    },
    test(
      "4: The Authorization Endpoint returns the Authorization Response with the Authorization Code upon successfully processing the Authorization Request"
    ) {
      assertTrue(true)
    },
    test("5: The Wallet sends a Token Request to the Token Endpoint with the Authorization Code") {
      assertTrue(true)
    },
    test(
      "6: The Wallet sends a Credential Request to the Credential Issuer's Credential Endpoint with the Access Token"
    ) {
      assertTrue(true)
    },
  ) @@ sequential

  val preAutorizedCodeFlowSpec = suite("Pre-Authorized Code Flow")(
    test(
      "1: The Credential Issuer successfully obtains consent and End-User data required for the issuance of a requested Credential from the End-User using an Issuer-specific business process"
    ) {
      assertTrue(true)
    },
    test(
      "2: Credential Issuer generates a Credential Offer for certain Credential(s) and communicates it to the Wallet"
    ) {
      assertTrue(true)
    },
    test("3: The Wallet uses the Credential Issuer's URL to fetch its metadata") {
      assertTrue(true)
    },
    test(
      "4: The Wallet sends the Pre-Authorized Code obtained in Step (2) in the Token Request to the Token Endpoint"
    ) {
      assertTrue(true)
    },
    test("5: The Wallet sends a Token Request to the Token Endpoint with the Authorization Code") {
      assertTrue(true)
    },
    test(
      "6: The Wallet sends a Credential Request to the Credential Issuer's Credential Endpoint with the Access Token"
    ) {
      assertTrue(true)
    },
  ) @@ sequential
}
