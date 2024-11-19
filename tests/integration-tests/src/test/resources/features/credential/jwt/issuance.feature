@jwt @issuance
Feature: Issue JWT credential


  Scenario: Issuing jwt credential with published PRISM DID
    Given Issuer and Holder have an existing connection
    And Issuer has a published DID for JWT
    And Holder has an unpublished DID for JWT
    When Issuer offers a jwt credential to Holder with "short" form DID
    And Holder receives the credential offer
    And Holder accepts jwt credential offer
    And Issuer issues the credential
    Then Holder receives the issued credential

  Scenario: Issuing jwt credential with published PRISM DID using Ed25519
    Given Issuer and Holder have an existing connection
    And Issuer has a published DID for JWT
    And Holder has an unpublished DID for JWT
    When Issuer offers a jwt credential to Holder with "short" form DID using issuingKid "assertion-2"
    And Holder receives the credential offer
    And Holder accepts jwt credential offer with keyId "auth-2"
    And Issuer issues the credential
    Then Holder receives the issued credential

  Scenario: Issuing jwt credential with a schema
    Given Issuer and Holder have an existing connection
    And Issuer has a published DID for JWT
    And Issuer has published STUDENT_SCHEMA schema
    And Holder has an unpublished DID for JWT
    When Issuer offers a jwt credential to Holder with "short" form using STUDENT_SCHEMA schema
    And Holder receives the credential offer
    And Holder accepts jwt credential offer
    And Issuer issues the credential
    Then Holder receives the issued credential

  Scenario: Issuing jwt credential with wrong claim structure for schema
    Given Issuer and Holder have an existing connection
    And Issuer has a published DID for JWT
    And Issuer has published STUDENT_SCHEMA schema
    And Holder has an unpublished DID for JWT
    When Issuer offers a jwt credential to Holder with "short" form DID with wrong claims structure using STUDENT_SCHEMA schema
    Then Issuer should see that credential issuance has failed

  Scenario: Issuing jwt credential with unpublished PRISM DID
    Given Issuer and Holder have an existing connection
    And Issuer has an unpublished DID for JWT
    And Holder has an unpublished DID for JWT
    And Issuer offers a jwt credential to Holder with "long" form DID
    And Holder receives the credential offer
    And Holder accepts jwt credential offer
    And Issuer issues the credential
    Then Holder receives the issued credential

  Scenario: Connectionless issuance of JWT credential using OOB invitation
    Given Issuer has a published DID for JWT
    And Holder has an unpublished DID for JWT
    When Issuer creates a "JWT" credential offer invitation with "short" form DID
    And Holder accepts the credential offer invitation from Issuer
    And Holder accepts jwt credential offer
    And Issuer issues the credential
    Then Holder receives the issued credential