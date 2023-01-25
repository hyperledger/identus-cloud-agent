@RFC0160 @AIP10
Feature: Agents connection

  Scenario: Establish a connection between two agents
    When Acme generates a connection invitation to Bob
    And Bob receives the connection invitation from Acme
    And Bob sends a connection request to Acme
    And Acme receives the connection request
    And Acme sends a connection response to Bob
    And Bob receives the connection response
    # And Bob sends <message> to Acme
    Then Acme and Bob have a connection
