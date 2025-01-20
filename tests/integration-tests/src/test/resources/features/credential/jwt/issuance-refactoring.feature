@jwt @issuance @refactoring
Feature: Issue JWT credential using different REST API request versions

  Background:
    Given Issuer and Holder have an existing connection
    And Issuer has a published DID for 'JWT'
    And Issuer has a published 'STUDENT_SCHEMA' schema
    And Holder has an unpublished DID for 'JWT'

  Scenario Outline: Issuing jwt credential using different API version
    When Issuer prepares the credential in 'JWT_VCDM_1_1' format using the '<createCredentialOfferApiVersion>' API
    And Issuer prepares to use a 'short' form of DID with key id 'assertion-1'
    And Issuer prepares the 'STUDENT_SCHEMA' to issue the credential
    And Issuer prepares the claims '<claims>' for the credential
    And Issuer sends the prepared credential offer to Holder
    And Holder receives the credential offer
    And Holder accepts jwt credential offer using 'auth-1' key id
    And Issuer issues the credential
    Then Holder receives the issued credential
    Examples:
      | createCredentialOfferApiVersion | claims         |
      | V0                              | STUDENT_CLAIMS |
      | V1                              | STUDENT_CLAIMS |

