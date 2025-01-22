@anoncred @issuance @refactoring
Feature: Issue Anoncreds credential using different REST API request versions

  Background:
    Given Issuer and Holder have an existing connection
    And Issuer has a published DID for 'ANONCRED'
    And Issuer has an anoncred schema definition
    And Holder has an unpublished DID for 'ANONCRED'

  Scenario Outline: Issuing jwt credential using different API version
    When Issuer prepares the credential in 'ANONCREDS_V1' format using the '<createCredentialOfferApiVersion>' API
    And Issuer prepares to use a 'short' form of DID with key id 'assertion-1'
    And Issuer prepares the claims '<claims>' for the credential
    And Issuer sends the prepared anoncreds credential offer to Holder
    And Holder receives the credential offer
    And Holder accepts anoncred credential offer
    And Issuer issues the credential
    Then Holder receives the issued credential
    Examples:
      | createCredentialOfferApiVersion | claims         |
      | V0                              | ANONCREDS_STUDENT_CLAIMS |
      | V1                              | ANONCREDS_STUDENT_CLAIMS |

