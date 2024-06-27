@RFC0453 @AIP20 @credentials
Feature: Issue Anoncred with published DID

  Background:
    Given Issuer and Holder have an existing connection
    And Issuer has a published DID for ANONCRED
    And Holder has an unpublished DID for ANONCRED

  Scenario: Issuing anoncred with published PRISM DID
    Given Issuer has an anoncred schema definition
    When Issuer offers anoncred to Holder
    And Holder receives the credential offer
    And Holder accepts credential offer for anoncred
    And Issuer issues the credential
    Then Holder receives the issued credential
