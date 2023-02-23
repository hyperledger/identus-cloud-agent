@RFC0453 @AIP20
Feature: Issue Credentials Protocol

  Scenario: Issuing credential with published PRISM DID to unpublished PRISM DID
    Given Acme and Bob have an existing connection
    When Acme creates unpublished DID
    And He publishes DID to ledger
    And Bob creates unpublished DID
    And Acme offers a credential to Bob
    And Bob receives the credential offer and accepts
    And Acme issues the credential
    Then Bob receives the issued credential

  Scenario: Issuing credential with published PRISM DID to published PRISM DID
    Given Acme and Bob have an existing connection
    When Acme creates unpublished DID
    And He publishes DID to ledger
    And Bob creates unpublished DID
    And He publishes DID to ledger
    And Acme offers a credential to Bob
    And Bob receives the credential offer and accepts
    And Acme issues the credential
    Then Bob receives the issued credential
