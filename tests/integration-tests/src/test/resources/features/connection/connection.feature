@connection @create
Feature: Agents connection

  Scenario: Establish a connection between two agents
    When Issuer generates a connection invitation to Holder
    And Holder sends a connection request to Issuer
    And Issuer receives the connection request and sends back the response
    And Holder receives the connection response
    Then Issuer and Holder have a connection
