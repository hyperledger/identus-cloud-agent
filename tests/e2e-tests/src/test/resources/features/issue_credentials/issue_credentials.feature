@RFC0453 @AIP20
Feature: Issue Credentials Protocol

  Background:
    Given Acme creates unpublished DID
    And He publishes DID to ledger
    And He sees DID successfully published to ledger

    Given Bob creates unpublished DID
    And He publishes DID to ledger
    And He sees DID successfully published to ledger

  Scenario: Issue a credential with the Issuer beginning with an offer
    Given Acme and Bob have an existing connection
    When Acme offers a credential to Bob
    And Bob receives the credential offer and accepts
    And Acme issues the credential
    # And Bob acknowledges the credential issue
    Then Bob receives the issued credential
