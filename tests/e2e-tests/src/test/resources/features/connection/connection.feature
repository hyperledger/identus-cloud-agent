@RFC0160 @AIP10
Feature: RFC 0160 Agent connection functions

  @T001-RFC0160 @critical @AcceptanceTest
  Scenario: Establish a connection between two agents
    When Acme generates a connection invitation
    And Bob receives the connection invitation
    And Bob sends a connection request to Acme
    And Acme receives the connection request
    And Acme sends a connection response to Bob
    And Bob receives the connection response
    # And Bob sends <message> to Acme
    Then Acme and Bob have a connection
