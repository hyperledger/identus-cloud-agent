@RFC0453 @AIP20
Feature: RFC0453 Issue Credentials Protocol

  Scenario: Issue a credential with the Issuer beginning with an offer
    And Acme and Bob have an existing connection
    When Acme offers a credential
    And Bob requests the credential
    And Acme issues the credential
    # And Bob acknowledges the credential issue
    Then Bob has the credential issued
