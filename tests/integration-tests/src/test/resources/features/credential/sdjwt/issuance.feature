@sdjwt @issuance
Feature: Issue SD-JWT credential

  Scenario: Issuing sd-jwt credential
    Given Issuer and Holder have an existing connection
    And Issuer has a published DID for SD_JWT
    And Holder has an unpublished DID for SD_JWT
    When Issuer offers a sd-jwt credential to Holder
    And Holder receives the credential offer
    And Holder accepts credential offer for sd-jwt
    And Issuer issues the credential
    Then Holder receives the issued credential
    And Holder checks the sd-jwt credential contents

  Scenario: Issuing sd-jwt credential with holder binding
    Given Issuer and Holder have an existing connection
    And Issuer has a published DID for SD_JWT
    And Holder has an unpublished DID for SD_JWT
    When Issuer offers a sd-jwt credential to Holder
    And Holder receives the credential offer
    And Holder accepts credential offer for sd-jwt with 'auth-1' key binding
    And Issuer issues the credential
    Then Holder receives the issued credential
    Then Holder checks the sd-jwt credential contents with holder binding

  Scenario: Connectionless issuance of sd-jwt credential with holder binding
    And Issuer has a published DID for SD_JWT
    And Holder has an unpublished DID for SD_JWT
    When Issuer creates a "SDJWT" credential offer invitation with "short" form DID
    And Holder accepts the credential offer invitation from Issuer
    And Holder accepts credential offer for sd-jwt with 'auth-1' key binding
    And Issuer issues the credential
    Then Holder receives the issued credential
    Then Holder checks the sd-jwt credential contents with holder binding


#  Scenario: Issuing sd-jwt with wrong algorithm
#    Given Issuer and Holder have an existing connection
#    When Issuer prepares a custom PRISM DID
#    And Issuer adds a 'secp256k1' key for 'assertionMethod' purpose with 'assert-1' name to the custom PRISM DID
#    And Issuer adds a 'secp256k1' key for 'authentication' purpose with 'auth-1' name to the custom PRISM DID
#    And Issuer creates the custom PRISM DID
#    And Holder has an unpublished DID for SD_JWT
#    And Issuer offers a sd-jwt credential to Holder
#    And Holder receives the credential offer
#    And Holder accepts credential offer for sd-jwt
#    And Issuer tries to issue the credential
#    Then Issuer should see that credential issuance has failed
