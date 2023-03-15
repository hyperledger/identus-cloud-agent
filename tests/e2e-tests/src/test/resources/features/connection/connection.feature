@RFC0160 @AIP10
Feature: Agents connection

@TEST_ATL-3834 
Scenario: Establish a connection between two agents
  When Acme generates a connection invitation to Bob
  And Bob receives the connection invitation from Acme
  And Bob sends a connection request to Acme
  And Acme receives the connection request and sends back the response
  And Bob receives the connection response
  Then Acme and Bob have a connection
