@sdjwt @issuance
Feature: Issue SD-JWT credential

  Scenario Outline: Issuing sd-jwt credential
    Given Issuer and Holder have an existing connection
    And Holder has an unpublished DID for 'SD_JWT'
    When Issuer prepares a custom PRISM DID
    And Issuer adds a '<assertionMethod>' key for 'assertionMethod' purpose with '<assertionName>' name to the custom PRISM DID
    And Issuer adds a '<authentication>' key for 'authentication' purpose with '<authenticationName>' name to the custom PRISM DID
    And Issuer creates the custom PRISM DID
    And Issuer publishes DID to ledger
    And Issuer has a published 'ID_SCHEMA' schema
    When Issuer offers a sd-jwt credential to Holder
    And Holder receives the credential offer
    And Holder accepts credential offer for sd-jwt
    And Issuer issues the credential
    Then Holder receives the issued credential
    And Holder checks the sd-jwt credential contents
    Examples:
      | assertionMethod | assertionName | authentication | authenticationName |
      | ed25519         | assert-1      | ed25519        | auth-1             |

  Scenario: Issuing sd-jwt credential with holder binding
    Given Issuer and Holder have an existing connection
    And Issuer has a published DID for 'SD_JWT'
    And Issuer has a published 'ID_SCHEMA' schema
    And Holder has an unpublished DID for 'SD_JWT'
    When Issuer offers a sd-jwt credential to Holder
    And Holder receives the credential offer
    And Holder accepts credential offer for sd-jwt with 'auth-1' key binding
    And Issuer issues the credential
    Then Holder receives the issued credential
    Then Holder checks the sd-jwt credential contents with holder binding

  Scenario: Connectionless issuance of sd-jwt credential with holder binding
    And Issuer has a published DID for 'SD_JWT'
    And Issuer has a published 'ID_SCHEMA' schema
    And Holder has an unpublished DID for 'SD_JWT'
    When Issuer creates a 'SDJWT' credential offer invitation with 'short' form DID and ID_SCHEMA schema
    And Holder accepts the credential offer invitation from Issuer
    And Holder accepts credential offer for sd-jwt with 'auth-1' key binding
    And Issuer issues the credential
    Then Holder receives the issued credential
    Then Holder checks the sd-jwt credential contents with holder binding
