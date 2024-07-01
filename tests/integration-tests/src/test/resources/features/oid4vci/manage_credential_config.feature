@oid4vci
Feature: Manage OID4VCI credential configuration

Background:
    Given Issuer has a published DID for JWT
    And Issuer has published STUDENT_SCHEMA schema
    And Issuer has an existing oid4vci issuer

Scenario: Successfully create credential configuration
    When Issuer uses STUDENT_SCHEMA to create a credential configuration "StudentProfile"
    Then Issuer sees the "StudentProfile" configuration on IssuerMetadata endpoint

Scenario: Successfully delete credential configuration
    Given Issuer has "StudentProfile" credential configuration created from STUDENT_SCHEMA
    When Issuer deletes "StudentProfile" credential configuration
    Then Issuer cannot see the "StudentProfile" configuration on IssuerMetadata endpoint
