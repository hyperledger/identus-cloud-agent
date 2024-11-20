@oid4vci
Feature: Issue JWT Credentials using OID4VCI authorization code flow

  Background:
    Given Issuer has a published DID for OIDC_JWT
    And Issuer has published STUDENT_SCHEMA schema
    And Issuer has an existing oid4vci issuer
    And Issuer has "StudentProfile" credential configuration created from STUDENT_SCHEMA

  Scenario: Issuing credential with published PRISM DID
    When Issuer creates an offer using "StudentProfile" configuration with "short" form DID
    And Holder receives oid4vci offer from Issuer
    And Holder resolves oid4vci issuer metadata and login via front-end channel
    And Holder presents the access token with JWT proof on CredentialEndpoint
    Then Holder sees credential issued successfully from CredentialEndpoint

  Scenario: Issuing credential with unpublished PRISM DID
    When Issuer creates an offer using "StudentProfile" configuration with "long" form DID
    And Holder receives oid4vci offer from Issuer
    And Holder resolves oid4vci issuer metadata and login via front-end channel
    And Holder presents the access token with JWT proof on CredentialEndpoint
    Then Holder sees credential issued successfully from CredentialEndpoint
