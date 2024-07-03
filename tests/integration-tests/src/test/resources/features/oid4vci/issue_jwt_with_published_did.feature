@oid4vci @dev
Feature: Issue JWT Credentials with published DID (OID4VCI)

Background:
    Given Issuer has a published DID for JWT
    And Issuer has published STUDENT_SCHEMA schema
    And Issuer has an existing oid4vci issuer
    And Issuer has "StudentProfile" credential configuration created from STUDENT_SCHEMA

Scenario: Issuing credential with published PRISM DID
    When Issuer creates an offer using "StudentProfile" configuration with "short" form DID
    And Holder receives oid4vci offer from Issuer
    And Holder resolves oid4vci issuer metadata and prepare AuthorizationRequest
