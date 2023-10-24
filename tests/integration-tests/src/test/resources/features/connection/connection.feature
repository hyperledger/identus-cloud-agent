Feature: Agents connection

Scenario: Establish a connection between two agents
  When Acme generates a connection invitation to Bob
  And Bob sends a connection request to Acme
  And Acme receives the connection request and sends back the response
  And Bob receives the connection response
  Then Acme and Bob have a connection
