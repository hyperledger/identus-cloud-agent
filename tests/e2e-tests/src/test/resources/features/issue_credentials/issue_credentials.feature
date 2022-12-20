@RFC0453 @AIP20
Feature: Issue Credentials Protocol

  Scenario: Issue a credential with the Issuer beginning with an offer
    Given Acme and Bob have an existing connection
    When Acme offers a credential to Bob
    And Bob requests the credential
    And Acme issues the credential
    # And Bob acknowledges the credential issue
    Then Bob has the credential issued
