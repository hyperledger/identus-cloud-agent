@sdjwt @issuance @refactoring
Feature: Issue SD-JWT credential using different REST API request versions

  Background:
    Given Issuer and Holder have an existing connection
    And Issuer has a published DID for 'SD_JWT'
    And Issuer has a published 'ID_SCHEMA' schema
    And Holder has an unpublished DID for 'SD_JWT'

  Scenario Outline: Issuing sd-jwt credential
    When Issuer prepares the credential in 'SD_JWT_VCDM_1_1' format using the '<createCredentialOfferApiVersion>' API
    And Issuer prepares to use a 'short' form of DID with key id 'assertion-1'
    And Issuer prepares the 'ID_SCHEMA' to issue the credential
    And Issuer prepares the claims '<claims>' for the credential
    And Issuer sends the prepared credential offer to Holder
    And Holder receives the credential offer
    And Holder accepts credential offer for sd-jwt
    And Issuer issues the credential
    Then Holder receives the issued credential
    And Holder checks the sd-jwt credential contents
    Examples:
      | createCredentialOfferApiVersion | claims    |
      | V0                              | ID_CLAIMS |
      | V1                              | ID_CLAIMS |
